/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.supplier.freenet;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for handling sms by freenet
 */
public class FreenetSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    Pattern NUMBERS_JSON_PATTERN = Pattern.compile("\"menuRows\":\\[(.+?)\\]");
    Pattern NUMBER_SUBSTRING_PATTERN = Pattern.compile("\\{(.+?)\\}");
    private FreenetOptionProvider provider;

    private static final String LOGIN_URL = "https://auth.freenet.de/portal/login.php";
    private static final String HOME_URL = "http://webmail.freenet.de/login/index.html";
    private static final String REFRESH_URL = "http://webmail.freenet.de/Global/Action/StatusBarGet";
    private static final String SEND_URL = "http://webmail.freenet.de/Sms/Action/Send?myAction=send&";
    private static final String BALANCE_URL = "http://webmail.freenet.de/Sms/View/Send";
    private List<String> sessionCookies;
    private static final String ENCODING = "ISO-8859-1";
    private static final int DEFAULT_WO_SI = 0;
    private static final int DEFAULT_W_SI = 1;
    private static final int QUICK_WO_SI = 2;
    private static final int QUICK_W_SI = 3;

    public FreenetSupplier() {
        provider = new FreenetOptionProvider(this);
    }


    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        return refreshInformations(false);
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException {
        return refreshInformations(true);
    }

    private synchronized SMSActionResult refreshInformations(boolean noLoginBefore) throws IOException {
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(REFRESH_URL, UrlConnectionFactory.METHOD_GET);
        factory.setCookies(sessionCookies);
        HttpURLConnection con = factory.create();
        return processRefreshReturn(con.getInputStream());
    }

    private SMSActionResult processRefreshReturn(InputStream is) throws IOException {
        String message = UrlConnectionFactory.inputStream2DebugString(is, ENCODING);
        String out = provider.getTextByResourceId(R.string.text_refresh_informations);
        Pattern p = Pattern.compile("SMS.*?\\}"); //get the SMS JSON object
        Matcher m = p.matcher(message);
        while (m.find()) {
            String messageJSONObject = message.substring(m.start(), m.end());
            p = Pattern.compile("[0-9]+");
            m = p.matcher(messageJSONObject);
            Integer allSMS = null;
            while (m.find()) {
                if (allSMS == null) {
                    allSMS = Integer.parseInt(messageJSONObject.substring(m.start(), m.end()));
                } else {
                    break;
                }
            }
            if (allSMS != null) {
                return SMSActionResult.NO_ERROR(String.format(out, allSMS));
            }

        }
        return SMSActionResult.UNKNOWN_ERROR();
    }


    private SMSActionResult processFireSMSReturn(InputStream is) throws IOException {
        Document parse = Jsoup.parse(is, ENCODING, "");
        Elements select = parse.select("input[name=SMSnotify]");
        if (select.size() != 1) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        String message = select.attr("value");
        if (message.contains("erfolgreich")) {
            return SMSActionResult.NO_ERROR(message);
        } else {
            return SMSActionResult.UNKNOWN_ERROR(message);
        }
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }


    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException {
        sessionCookies = new ArrayList<String>(2);
        String tmpUrl;
        if (userName != null && !userName.contains("@")) {
            userName += "@freenet.de";
        }
        tmpUrl = LOGIN_URL + "?username=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&password=" + URLEncoder.encode(password == null ? "" : password, ENCODING);

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        HttpURLConnection httpURLConnection = factory.create();
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        if (headerFields == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String sidCookie = UrlConnectionFactory.findCookieByName(headerFields, "SID");
        if (sidCookie == null) {
            SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.login_failed));
            smsActionResult.setRetryMakesSense(false);
            return smsActionResult;
        }
        sessionCookies.add(sidCookie);


        //now other cookies needed, too
        factory = new UrlConnectionFactory(HOME_URL);
        factory.setCookies(sessionCookies);
        HttpURLConnection httpURLConnection1 = factory.create();
        Map<String, List<String>> homeHeaderFields = httpURLConnection1.getHeaderFields();
        if (homeHeaderFields == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        List<String> otherCookies = UrlConnectionFactory.findCookiesByPattern(homeHeaderFields, ".*");
        if (otherCookies == null || otherCookies.size() == 0) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        sessionCookies.addAll(otherCookies);
        if (sessionCookies.size() < 2) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        return SMSActionResult.LOGIN_SUCCESSFUL();


    }


    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        return sendSMS(smsText, receivers, null, spinnerText);
    }

    private FireSMSResultList sendSMS(String smsText, List<Receiver> receivers, DateTimeObject dateTime, String spinnerText) throws IOException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            provider.saveState();
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }

        String message = URLEncoder.encode(smsText, ENCODING);
        int sendMethod = findSendMethod(spinnerText);
        String sender = null;
        if (sendMethod == DEFAULT_W_SI || sendMethod == QUICK_W_SI) {
            sender = provider.getSender();
            if (sender == null) {
                provider.saveState();
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.refresh_sender_first)), receivers);
            } else {
                sender = URLEncoder.encode(sender, ENCODING);
            }
        }
        FireSMSResultList out = new FireSMSResultList(receivers.size());
        //currently only free sms supported, for paid accounts change will be here
        for (Receiver receiver : receivers) {
            StringBuilder tmpUrl = new StringBuilder(SEND_URL);
            tmpUrl.append("senderName=service%40freenet.de&defaultEmailSender=&to=");
            tmpUrl.append(receiver.getReceiverNumber()).append("&smsText=").append(message);
            if (dateTime != null) {
                tmpUrl.append(String.format("&later=1&day=%02d&month=%02d&year=%d&hours=%02d&minutes=%02d", dateTime.getDay(), dateTime.getMonth() + 1, dateTime.getYear(), dateTime.getHour(), dateTime.getMinute()));
            }
            if (sendMethod == DEFAULT_W_SI || sendMethod == QUICK_W_SI) {
                tmpUrl.append("&from=").append(sender);
            }
            if (sendMethod == QUICK_WO_SI || sendMethod == QUICK_W_SI) {
                tmpUrl.append("&quick=1");
            }
            if (provider.getSettings().getBoolean(FreenetOptionProvider.PROVIDER_SAVE_IN_SENT, false)) {
                tmpUrl.append("&smsToSent=1");
            }
            try {
                UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl.toString());
                factory.setCookies(sessionCookies);
                HttpURLConnection con = factory.create();
                out.add(new FireSMSResult(receiver, processFireSMSReturn(con.getInputStream())));
            } catch (SocketTimeoutException stoe) {
                provider.saveState();
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                provider.saveState();
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }
        }
        FireSMSResultList.SendResult sendResult = out.getResult();
        if (sendResult.equals(FireSMSResultList.SendResult.SUCCESS) || sendResult.equals(FireSMSResultList.SendResult.BOTH)) {
            provider.saveLastSender();
        } else {
            provider.saveState();
        }
        return out;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException {
        return sendSMS(smsText, receivers, dateTime, spinnerText);
    }

    @Override
    public int getMinuteStepSize() {
        return 1;
    }

    @Override
    public int getDaysInFuture() {
        return 400;
    }

    @Override
    public boolean isSendTypeTimeShiftCapable(String spinnerText) {
        return true;
    }


    public SMSActionResult resolveNumbers() throws IOException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return result;
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_URL);
        factory.setCookies(sessionCookies);
        HttpURLConnection con = factory.create();

        Map<String, String> numbers = new TreeMap<String, String>();

        String returnAsString = UrlConnectionFactory.inputStream2DebugString(con.getInputStream(), ENCODING);
        Matcher m = NUMBERS_JSON_PATTERN.matcher(returnAsString);
        while (m.find()) {
            String s = m.group(1);
            Matcher matcher = NUMBER_SUBSTRING_PATTERN.matcher(s);
            while (matcher.find()) {
                String matchingRow = matcher.group(1);
                String numberPresentation = matchingRow.replaceAll(".*rowText\":\"", "");
                numberPresentation = numberPresentation.replaceAll("\".*", "");
                String number = matchingRow.replaceAll(".*EMO_Sms.setSender\\('", "");
                number = number.replaceAll("'.*", "");
                //replace all valid characters to check if its really a number and not the mail address
                String tmpNumber = number.replaceAll("\\+", "").replaceAll("\\(", "").replaceAll("\\)", "").replaceAll(" ", "").replaceAll("/", "").replaceAll("\\\\", "");
                if (tmpNumber.matches("\\d+")) {    //just numbers
                    numbers.put(numberPresentation, number);

                }

            }
        }

        if (numbers.isEmpty()) {
            return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_senders_available));
        }
        provider.saveNumbers(numbers);
        return SMSActionResult.NO_ERROR();
    }


    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = DEFAULT_WO_SI;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i;
            }
        }

        return sendType;
    }
}
