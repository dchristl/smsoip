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

import android.util.Log;
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
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for handling sms by freenet
 */
public class FreenetSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private FreenetOptionProvider provider;

    private static final String LOGIN_URL = "https://auth.freenet.de/portal/login.php";
    private static final String HOME_URL = "http://webmail.freenet.de/login/index.html";
    private static final String REFRESH_URL = "http://webmail.freenet.de/Global/Action/StatusBarGet";
    private static final String SEND_URL = "http://webmail.freenet.de/Sms/Action/Send?myAction=send&";
    private List<String> sessionCookies;
    private static final String ENCODING = "ISO-8859-1";

    public FreenetSupplier() {
        provider = new FreenetOptionProvider();
    }


    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        return refreshInformations(false);
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException {
        return refreshInformations(true);
    }

    private SMSActionResult refreshInformations(boolean noLoginBefore) throws IOException {
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        String tmpUrl = REFRESH_URL;
        HttpURLConnection con;
        con = (HttpURLConnection) new URL(tmpUrl).openConnection();
        con.setReadTimeout(TIMEOUT);
        con.setConnectTimeout(TIMEOUT);
        con.setRequestProperty("User-Agent", TARGET_AGENT);
        con.setRequestMethod("GET");
        StringBuilder cookieBuilder = new StringBuilder();
        for (String sessionCookie : sessionCookies) {
            cookieBuilder.append(sessionCookie).append(";");
        }
        con.setRequestProperty("Cookie", cookieBuilder.toString());
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
            Integer paidSMS = null;
            while (m.find()) {
                if (allSMS == null) {
                    allSMS = Integer.parseInt(messageJSONObject.substring(m.start(), m.end()));
                } else if (paidSMS == null) {
                    paidSMS = Integer.parseInt(messageJSONObject.substring(m.start(), m.end()));
                } else {
                    break;
                }
            }
            if (allSMS != null && paidSMS != null) {
                return SMSActionResult.NO_ERROR(String.format(out, allSMS, paidSMS));
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
        tmpUrl = LOGIN_URL + "?username=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&password=" + URLEncoder.encode(password == null ? "" : password, ENCODING);

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        HttpURLConnection httpURLConnection = factory.create();
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        if (headerFields == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String sidCookie = UrlConnectionFactory.findCookieByName(headerFields, "SID");
        if (sidCookie == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
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
        return sendSMS(smsText, receivers, null);
    }

    //    myAction=send&from=&senderName=service%40freenet.de&defaultEmailSender=&to=01745686886&smsText=Test+zeitversetzt&later=1&day=24&month=07&year=2012&hours=22&minutes=09&smsToSent=1
    private FireSMSResultList sendSMS(String smsText, List<Receiver> receivers, DateTimeObject dateTime) throws IOException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        String message = URLEncoder.encode(smsText, ENCODING);

        FireSMSResultList out = new FireSMSResultList(receivers.size());
        //currently only free sms supported, for paid accounts change will be here
        for (Receiver receiver : receivers) {
            String tmpUrl = SEND_URL + "&senderName=service%40freenet.de&defaultEmailSender=&to=" + receiver.getReceiverNumber() + "&smsText=" + message;
            if (dateTime != null) {
                tmpUrl += String.format("&later=1&day=%02d&month=%02d&year=%d&hours=%02d&minutes=%02d", dateTime.getDay(), dateTime.getMonth() + 1, dateTime.getYear(), dateTime.getHour(), dateTime.getMinute());
            }
            if (provider.getSettings().getBoolean(FreenetOptionProvider.PROVIDER_SAVE_IN_SENT, false)) {
                tmpUrl += "&smsToSent=1";
            }
            try {
                UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
                factory.setCookies(sessionCookies);
                HttpURLConnection con = factory.create();
                out.add(new FireSMSResult(receiver, processFireSMSReturn(con.getInputStream())));
            } catch (SocketTimeoutException stoe) {
                Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }
        }
        return out;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException {
        return sendSMS(smsText, receivers, dateTime);
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
}
