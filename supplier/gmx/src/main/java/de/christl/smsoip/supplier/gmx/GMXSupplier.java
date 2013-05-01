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

package de.christl.smsoip.supplier.gmx;

import android.os.Build;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import de.christl.smsoip.supplier.gmx.util.Base64;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class GMXSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private static final String USER_AGENT = "Dalvik/1.4.0 (Linux; U; Android " + Build.VERSION.RELEASE + "; Galaxy GMX SMS/2.0.7 (Production; ReleaseBuild; de-de)";

    private GMXOptionProvider provider;
    private static final String LOGIN_URL = "https://lts.gmx.net/logintokenserver-1.1/Logintoken/";
    private static final String LOGIN_BODY = "identifierUrn=%s&password=%s&durationType=PERMANENT&loginClientType=freemessage";

    private static final String TOKEN_LOGIN_URL = "https://uas2.uilogin.de/tokenlogin/";
    private static final String TOKEN_LOGIN_BODY = "serviceID=freemessage.gmxnet.live&logintoken=%s";

    private static final String FIND_NUMBERS_URL_START = "https://hsp.gmx.net/http-service-proxy1/service/number-verification-service-2/NumberVerified/urn:uasaccountid:accountId";
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*?\\{\"number\":\"([0-9]+)\",\"numberVerifyState\":\"VERIFIED\",\"defaultNumber\":(.*?)\\}.*?");

    private static final String INFO_URL = "https://sms-submission-service.gmx.de/sms-submission-service/gmx/sms/2.0/SmsCapabilities?";
    private static final Pattern INFO_PATTERN = Pattern.compile("MAX_MONTH_FREE_SMS=([0-9]+).*?AVAILABLE_FREE_SMS=([0-9]+).*?MONTH_PAY_SMS=([0-9]+).*");

    private static final String TARGET_URL = "https://sms-submission-service.gmx.de/sms-submission-service/gmx/sms/2.0/SmsSubmission?clientType=GMX_ANDROID&messageType=SMS&sourceNumber=%s&destinationNumber=%s&options=SEND_ERROR_NOTIFY_MAIL";


    private static final String ENCODING_ISO = "ISO-8859-15";
    private static final String ENCODING_UTF_8 = "UTF-8";

    private String sessionId;

    public GMXSupplier() {
        provider = new GMXOptionProvider(this);
    }

    GMXSupplier(GMXOptionProvider provider) {
        this.provider = provider;
    }

    /**
     * send the message with the given text
     *
     * @param smsText
     * @param receivers
     * @param dateTimeObject
     * @return
     * @throws IOException
     */
    public FireSMSResultList sendSMS(String smsText, List<Receiver> receivers, DateTimeObject dateTimeObject) throws IOException {

        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            provider.saveTemporaryState();
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        if (provider.getSettings().getBoolean(GMXOptionProvider.PROVIDER_CHECKNOFREESMSAVAILABLE, false)) {
            int availableFreeMessages = findAvailableFreeMessages(getInformations());
            boolean noFreeAvailable;
            if (availableFreeMessages != -1) {
                int messageLength = getProvider().getTextMessageLength();
                int smsCount = Math.round((smsText.length() / messageLength));
                smsCount = smsText.length() % messageLength == 0 ? smsCount : smsCount + 1;
                noFreeAvailable = !((receivers.size() * smsCount) <= availableFreeMessages);
            } else {
                provider.saveTemporaryState();
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.free_messages_could_not_resolved)), receivers);
            }
            if (noFreeAvailable) {
                provider.saveTemporaryState();
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_free_messages_available)), receivers);
            }
        }


        String sender = provider.getSender();
        if (sender == null) {
            provider.saveTemporaryState();
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.refresh_sender_first)), receivers);
        }
        FireSMSResultList out = new FireSMSResultList();
        String str = provider.getUserName() + ":" + provider.getPassword();
        final byte[] decodedBytes = Base64.encode(str.getBytes(ENCODING_UTF_8), Base64.NO_WRAP);
        for (Receiver receiver : receivers) {
            String shortReceiverNumber = receiver.getReceiverNumber().replaceAll("^00", "");
            String tmpUrl = String.format(TARGET_URL, sender, shortReceiverNumber);
            if (dateTimeObject != null) {       /*   check time implement better logic for login failure (message, token)*/
                //change the  device dependent calendar instance to the german one
                Calendar calendar = fixDate(dateTimeObject);
                tmpUrl += "&sendDate=" + calendar.getTimeInMillis();
            }
            UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
            Map<String, String> requestMap = new HashMap<String, String>() {
                {
                    put("X-UI-CallerIP", "127.0.0.1");
                    put("Content-Type", "text/plain; charset=UTF-8");
                    put("Authorization", "Basic " + new String(decodedBytes));
                }
            };
            factory.setRequestProperties(requestMap);
            factory.writeBody(smsText);
            InputStream inputStream = factory.getConnnection().getInputStream();
            SMSActionResult sendResult = processReturn(inputStream, shortReceiverNumber);
            if (sendResult.isSuccess()) {
                provider.saveLastSender();
            } else {
                provider.saveTemporaryState();
            }
            out.add(new FireSMSResult(receiver, sendResult));
        }
        return out;

    }

    /**
     * change the date object to the german one to fullfill crappy api
     *
     * @param dateTimeObject
     * @return
     */
    private Calendar fixDate(DateTimeObject dateTimeObject) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.set(Calendar.YEAR, dateTimeObject.getYear());
        calendar.set(Calendar.MONTH, dateTimeObject.getMonth());
        calendar.set(Calendar.DAY_OF_MONTH, dateTimeObject.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, dateTimeObject.getHour());
        calendar.set(Calendar.MINUTE, dateTimeObject.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * check if the receivernumber is available in response
     *
     * @throws IOException
     */
    SMSActionResult processReturn(InputStream is, String shortReceiverNumber) throws IOException {
        if (is == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        //fix if old numbers are saved
        shortReceiverNumber = shortReceiverNumber.replaceAll("^00", "").replaceAll("^\\+", "");
        String s = UrlConnectionFactory.inputStream2DebugString(is);
        if (s.contains(shortReceiverNumber + "=")) {
            return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.sentSuccess));
        }
        return SMSActionResult.UNKNOWN_ERROR();

    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        SMSActionResult result = refreshInformations(false);
        if (result.isSuccess() && result.getMessage().equals("")) {              //informations are not available at first try so do it twice
            result = refreshInformations(false);
        }
        return result;

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
        String infoText = findInfoText(getInformations());
        return SMSActionResult.NO_ERROR(infoText);
    }

    private String getInformations() throws IOException {
        String str = provider.getUserName() + ":" + provider.getPassword();
        final byte[] decodedBytes = Base64.encode(str.getBytes(ENCODING_UTF_8), Base64.NO_WRAP);
        UrlConnectionFactory factory = new UrlConnectionFactory(INFO_URL, UrlConnectionFactory.METHOD_GET);
        Map<String, String> requestMap = new HashMap<String, String>() {
            {
                put("X-UI-CALLER-IP", "127.0.0.1");
                put("Accept", "application/x-www-form-urlencoded");
                put("Authorization", "Basic " + new String(decodedBytes));
            }
        };
        factory.setRequestProperties(requestMap);
        InputStream inputStream = factory.getConnnection().getInputStream();
        if (inputStream == null) {
            return null;
        }
        return UrlConnectionFactory.inputStream2DebugString(inputStream);
    }


    /**
     * MAX_MONTH_FREE_SMS=10&MONTH_FREE_SMS=1&LIMIT_MONTH_AUTO_SMS=0&AVAILABLE_FREE_SMS=9&USER_TYPE=GMX_FREEMAIL&MAX_WEBCENT=0&MONTH_PAY_SMS=0&MONTH_AUTO_SMS=0
     *
     * @param response - response from server
     * @return found info string or empty if not found
     * @throws IOException
     */
    private String findInfoText(String response) throws IOException {
        Matcher m = INFO_PATTERN.matcher(response);
        String maxMsgMonth = "0";
        String sentMsg = "0";
        String paySent = "0";
        while (m.find()) {
            maxMsgMonth = m.group(1);
            sentMsg = m.group(2);
            paySent = m.group(3);
        }
        String textByResourceId = provider.getTextByResourceId(R.string.infoText);
        return String.format(textByResourceId, sentMsg, maxMsgMonth, paySent);
    }

    /**
     * find just the free messages in the response
     *
     * @param response
     * @return
     */
    private int findAvailableFreeMessages(String response) {
        Matcher m = INFO_PATTERN.matcher(response);
        int sentMsg = -1;
        try {
            while (m.find()) {
                sentMsg = Integer.parseInt(m.group(2));
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        return sentMsg;
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException {
        String loginToken;
        sessionId = null;

        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        factory.setTargetAgent(USER_AGENT);
        Map<String, String> requestMap = new HashMap<String, String>() {
            {
                put("X-UI-APP", "GmxFreeMessageAndroid/2.0.7");
                put("X-UI-CallerIP", "127.0.0.1");
                put("Accept-Encoding", "gzip");
                put("Content-Type", "application/x-www-form-urlencoded");
            }
        };
        factory.setRequestProperties(requestMap);
        HttpURLConnection con = factory.writeBody(String.format(LOGIN_BODY, URLEncoder.encode("urn:identifier:mailto:" + userName, ENCODING_ISO), URLEncoder.encode(password, ENCODING_ISO)));

        //no network
        Map<String, List<String>> headerFields = con.getHeaderFields();
        InputStream tmpStream;
        try {
            tmpStream = con.getInputStream();
        } catch (FileNotFoundException e) {
            return getLoginFailedSMSActionResult(userName);
        }
        if (headerFields == null || tmpStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        GZIPInputStream gzipInputStream = new GZIPInputStream(tmpStream);
        loginToken = UrlConnectionFactory.inputStream2DebugString(gzipInputStream, ENCODING_UTF_8);
        if (!(loginToken == null || loginToken.length() == 0)) {
            //try to login by token
            factory = new UrlConnectionFactory(TOKEN_LOGIN_URL);
            factory.setTargetAgent(USER_AGENT);
            factory.setRequestProperties(requestMap);
            factory.setFollowRedirects(false);
            con = factory.writeBody(String.format(TOKEN_LOGIN_BODY, URLEncoder.encode("urn:token:freemessage:" + loginToken, ENCODING_UTF_8)));
            Map<String, List<String>> tokenHeader = con.getHeaderFields();

            if (con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                String locationHeader = null;
                Outer:
                for (Map.Entry<String, List<String>> stringListEntry : tokenHeader.entrySet()) {
                    String cookieList = stringListEntry.getKey();
                    if (cookieList != null && cookieList.equalsIgnoreCase("location")) {
                        for (String location : stringListEntry.getValue()) {
                            if (location != null) {
                                locationHeader = location;
                                break Outer;
                            }
                        }
                    }
                }
                if (locationHeader != null && locationHeader.contains("jsessionid=")) {
                    sessionId = locationHeader.replaceAll(".*jsessionid=", "JSESSIONID=");

                }
            } else {
                sessionId = UrlConnectionFactory.findCookieByPattern(tokenHeader, "JSESSIONID=.*");
            }

            if (!(sessionId == null || sessionId.length() == 0)) {
                //parseJSessionId
                provider.setAccountChanged(false);
                return SMSActionResult.LOGIN_SUCCESSFUL();
            }

        }

        return getLoginFailedSMSActionResult(userName);
    }

    /**
     * return the login failed message dependent if the username contains domain name
     *
     * @param userName
     * @return
     */
    private SMSActionResult getLoginFailedSMSActionResult(String userName) {
        SMSActionResult smsActionResult;
        if (userName == null || !userName.contains("@gmx.")) {
            smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.login_error));
        } else {
            smsActionResult = SMSActionResult.LOGIN_FAILED_ERROR();
        }
        smsActionResult.setRetryMakesSense(false);
        return smsActionResult;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException {
        return sendSMS(smsText, receivers, dateTime);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        return sendSMS(smsText, receivers, null);
    }

    @Override
    public int getMinuteStepSize() {
        return 5;
    }

    @Override
    public int getDaysInFuture() {
        return 365 * 5; //five years should be enough
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


        UrlConnectionFactory factory = new UrlConnectionFactory(FIND_NUMBERS_URL_START, UrlConnectionFactory.METHOD_GET);
        factory.setCookies(new ArrayList<String>() {
            {
                add(sessionId);
            }
        });

        factory.setTargetAgent(USER_AGENT);
        Map<String, String> requestMap = new HashMap<String, String>() {
            {
                put("X-UI-APP", "GmxFreeMessageAndroid/2.0.7");
                put("X-UI-CallerIP", "127.0.0.1");
                put("Accept-Encoding", "gzip");
                put("Accept", "application/json");
            }
        };
        factory.setRequestProperties(requestMap);
        InputStream inputStream = factory.getConnnection().getInputStream();
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }

        String content = UrlConnectionFactory.inputStream2DebugString(new GZIPInputStream(inputStream), ENCODING_ISO);
        Matcher m = NUMBER_PATTERN.matcher(content);
        HashMap<Integer, String> numbers = new HashMap<Integer, String>();
        int i = 1;
        while (m.find()) {
            //check the default number
            boolean defaultNumber = Boolean.parseBoolean(m.group(2));
            //put the default number as first (can only one exist
            numbers.put(defaultNumber ? 1 : ++i, m.group(1));
        }
        if (numbers.size() < 1) {
            return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_numbers_maintened));
        }

        provider.saveNumbers(numbers);
        return SMSActionResult.NO_ERROR();
    }

}
