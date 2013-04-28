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

import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import de.christl.smsoip.supplier.gmx.util.Base64;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class GMXSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private static final String USER_AGENT = "Dalvik/1.4.0 (Linux; U; Android 2.3.3; Galaxy GMX SMS/2.0.7 (Production; ReleaseBuild; de-de)";
    private GMXOptionProvider provider;
    private static final String LOGIN_URL = "https://lts.gmx.net/logintokenserver-1.1/Logintoken/";
    private static final String LOGIN_BODY = "identifierUrn=%s&password=%s&durationType=PERMANENT&loginClientType=freemessage";

    private static final String TOKEN_LOGIN_URL = "https://uas2.uilogin.de/tokenlogin/";
    private static final String TOKEN_LOGIN_BODY = "serviceID=freemessage.gmxnet.live&logintoken=%s";

    private static final String FIND_NUMBERS_URL_START = "https://hsp.gmx.net/http-service-proxy1/service/number-verification-service-2/NumberVerified/urn:uasaccountid:accountId";
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*?\\{\"number\":\"([0-9]+)\",\"numberVerifyState\":\"VERIFIED\",\"defaultNumber\":(.*?)\\}.*?");

    private static final String INFO_URL = "https://sms-submission-service.gmx.de/sms-submission-service/gmx/sms/2.0/SmsCapabilities?";
    private static final Pattern INFO_PATTERN = Pattern.compile("MAX_MONTH_FREE_SMS=([0-9]+).*?AVAILABLE_FREE_SMS=([0-9]+).*?MONTH_PAY_SMS=([0-9]+).*");
    /**
     * this.getClass().getCanonicalName() for output.
     */


    private static final String TARGET_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1-1.IBehaviorListener.0-main~tab-content~panel~container-content~panel-form-sendMessage&wicket-ajax=true&wicket-ajax-baseurl=home";

    private static final String SAVE_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1-1.IBehaviorListener.0-main~tab-content~panel~container-content~panel-form-send~date~panel-send~date~form-save&wicket-ajax=true&wicket-ajax-baseurl=home";
    private static final String FIND_NUMBERS_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1";


    private String loginToken;
    private static final String SESSION_ID_URL_STRING = "jsessionid";
    private String sendNowRadioButtonId;
    private String sendLaterRadioButtonId;
    private static final String CRLF = "\r\n";
    private static final String ENCODING = "ISO-8859-15";

    public static final int WITH_PHONE_NUMBER = 0;
    public static final int WITH_FREE_TEXT = 1;

    private Long leaseTime;
    private String sid;
    private String sessionId;

    public GMXSupplier() {
        provider = new GMXOptionProvider(this);
    }

    GMXSupplier(GMXOptionProvider provider) {
        this.provider = provider;
    }


    public FireSMSResultList sendSMS(String smsText, List<Receiver> receivers, DateTimeObject dateTimeObject, String spinnerText) throws IOException {
        if (isLoginNeeded()) {
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                provider.saveTemporaryState();
                return FireSMSResultList.getAllInOneResult(result, receivers);
            }
        }
        if (provider.getSettings().getBoolean(GMXOptionProvider.PROVIDER_CHECKNOFREESMSAVAILABLE, false)) {
            SMSActionResult tmpResult = refreshInformations(true);
            if (tmpResult.isSuccess()) {
                String userText = tmpResult.getMessage();
                String[] split = userText.split(" ");
                boolean noFreeAvailable;
                if (split.length > 3) {
                    int freeSMS;
                    try {
                        freeSMS = Integer.parseInt(split[3]);
                    } catch (NumberFormatException e) {
                        provider.saveTemporaryState();
                        return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.free_messages_could_not_resolved)), receivers);
                    }
                    int messageLength = getProvider().getTextMessageLength();
                    int smsCount = Math.round((smsText.length() / messageLength));
                    smsCount = smsText.length() % messageLength == 0 ? smsCount : smsCount + 1;
                    noFreeAvailable = !((receivers.size() * smsCount) <= freeSMS);
                } else {
                    provider.saveTemporaryState();
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.free_messages_could_not_resolved)), receivers);
                }
                if (noFreeAvailable) {
                    provider.saveTemporaryState();
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_free_messages_available)), receivers);
                }
            }
        }


        Map<String, String> parameterMap = new LinkedHashMap<String, String>();
        parameterMap.put("id8_hf_0", "");
        int sendMethod = findSendMethod(spinnerText);
        switch (sendMethod) {
            default:
            case WITH_PHONE_NUMBER:
                int senderId = provider.getSenderId();
                if (senderId == -1) {
                    provider.saveTemporaryState();
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.refresh_sender_first)), receivers);
                } else {
                    parameterMap.put("from", String.valueOf(senderId));
                }
                break;

            case WITH_FREE_TEXT:
                parameterMap.put("from", "0");
                String sender = provider.getSender();
                if (sender == null || sender.length() < 2) {
                    provider.saveTemporaryState();
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_free_input)), receivers);
                }
                parameterMap.put("custom-sender-string", sender);
                break;
        }


        StringBuilder receiverListBuilder = new StringBuilder();
        for (int i = 0, receiversSize = receivers.size(); i < receiversSize; i++) {
            String receiver = receivers.get(i).getReceiverNumber();
            receiverListBuilder.append(String.format("{;;%s}", receiver));
            if (i + 1 != receivers.size()) {
                receiverListBuilder.append(",");
            }
        }
        parameterMap.put("recipients", receiverListBuilder.toString());
        parameterMap.put("upload-panel:upload-form:file\"; filename=\"", "");
        parameterMap.put("subject", "");
        parameterMap.put("textMessage", smsText);
        String radioButtonId;
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        String dateString;
        String hour;
        String minute;
        if (dateTimeObject == null) {
            radioButtonId = sendNowRadioButtonId;
            dateString = sdf.format(new Date());
            hour = "0";
            minute = "0";
        } else {
            radioButtonId = sendLaterRadioButtonId;
            dateString = sdf.format(dateTimeObject.getCalendar().getTime());
            hour = String.valueOf(dateTimeObject.getHour());
            minute = String.valueOf(dateTimeObject.getMinute());
            parameterMap.put("send-date-panel:send-date-form:send-options-rdgrp", radioButtonId);
            parameterMap.put("send-date-panel:send-date-form:send-date", dateString);
            parameterMap.put("send-date-panel:send-date-form:send-date-hour", hour);
            parameterMap.put("send-date-panel:send-date-form:send-date-minute", minute);
            SMSActionResult smsActionResult = sendSaveRequest(parameterMap);
            if (!smsActionResult.isSuccess()) {
                provider.saveTemporaryState();
                return FireSMSResultList.getAllInOneResult(smsActionResult, receivers);
            }
        }
        parameterMap.put("send-date-panel:send-date-form:send-options-rdgrp", radioButtonId);
        parameterMap.put("send-date-panel:send-date-form:send-date", dateString);
        parameterMap.put("send-date-panel:send-date-form:send-date-hour", hour);
        parameterMap.put("send-date-panel:send-date-form:send-date-minute", minute);
        parameterMap.put("sendMessage", "1");

        String tmpUrl = String.format(TARGET_URL, loginToken);
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        factory.writeMultipartBody(parameterMap, ENCODING);
        HttpURLConnection connnection = factory.getConnnection();
        SMSActionResult result = processReturn(connnection.getInputStream());
        if (result.isSuccess()) {
            provider.saveLastSender();
        } else {
            provider.saveTemporaryState();
        }
        return FireSMSResultList.getAllInOneResult(result, receivers);

    }

    private SMSActionResult sendSaveRequest(Map<String, String> parameterMap) {

        Map<String, String> otherParameterMap = new HashMap<String, String>(parameterMap);
        otherParameterMap.put("send-date-panel:send-date-form:save", "1");
        try {
            String tmpUrl = String.format(SAVE_URL, loginToken);
            UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
            factory.writeMultipartBody(otherParameterMap, ENCODING);
            //just fire request
            factory.getConnnection().getInputStream();
            return SMSActionResult.NO_ERROR();

        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return SMSActionResult.NETWORK_ERROR();
        }
    }

    /**
     * its an ajax response and easier to handle than with JSoup
     *
     * @throws IOException
     */
    SMSActionResult processReturn(InputStream is) throws IOException {

        Document document = Jsoup.parse(is, ENCODING, "");
        String feedBackPanelText = document.select("component").text();

        Document feedBackPanelContent = Jsoup.parse(feedBackPanelText, ENCODING);
        Elements feedBackPanelDiv = feedBackPanelContent.select("div");
        if (feedBackPanelDiv.size() == 0) {
            leaseTime = null; //force a new login
            return SMSActionResult.LOGIN_FAILED_ERROR();
        } else if (feedBackPanelDiv.select("span").attr("class").equals("feedbackPanelERROR")) {
            return SMSActionResult.UNKNOWN_ERROR(feedBackPanelDiv.text());
        } else {
            return SMSActionResult.NO_ERROR(feedBackPanelDiv.select("#confirmation_message_text").text());
        }
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
        String str = provider.getUserName() + ":" + provider.getPassword();
        final byte[] decodedBytes = Base64.encode(str.getBytes("UTF-8"), Base64.NO_WRAP);
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
            return SMSActionResult.NETWORK_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        String infoText = findInfoText(response);
        return SMSActionResult.NO_ERROR(infoText);
    }

    private boolean isLoginNeeded() {
        boolean out = true;
        if (!provider.isAccountChanged()) {
            if (leaseTime != null) {
                out = leaseTime + (5 * 60 * 1000) - System.currentTimeMillis() < 0;
            }
        }
        return out;
    }

    /**
     * MAX_MONTH_FREE_SMS=10&MONTH_FREE_SMS=1&LIMIT_MONTH_AUTO_SMS=0&AVAILABLE_FREE_SMS=9&USER_TYPE=GMX_FREEMAIL&MAX_WEBCENT=0&MONTH_PAY_SMS=0&MONTH_AUTO_SMS=0
     *
     * @param response
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


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException {
        leaseTime = null;
        loginToken = null;
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
        HttpURLConnection con = factory.writeBody(String.format(LOGIN_BODY, URLEncoder.encode("urn:identifier:mailto:" + userName, ENCODING), URLEncoder.encode(password, ENCODING)));

        //no network
        Map<String, List<String>> headerFields = con.getHeaderFields();
        InputStream tmpStream = con.getInputStream();
        if (headerFields == null || tmpStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        GZIPInputStream gzipInputStream = new GZIPInputStream(tmpStream);
        loginToken = UrlConnectionFactory.inputStream2DebugString(gzipInputStream, ENCODING);
        if (!(loginToken == null || loginToken.length() == 0)) {
            leaseTime = System.currentTimeMillis();
            //try to login by token
            factory = new UrlConnectionFactory(TOKEN_LOGIN_URL);
            con = factory.writeBody(String.format(TOKEN_LOGIN_BODY, URLEncoder.encode("urn:token:freemessage:" + loginToken, ENCODING)));
            Map<String, List<String>> tokenHeader = con.getHeaderFields();
            sessionId = UrlConnectionFactory.findCookieByPattern(tokenHeader, "JSESSIONID=.*");

            if (!(sessionId == null || sessionId.length() == 0)) {
                //parseJSessionId
//                sessionId = sessionId.replaceAll("JSESSIONID=", "").replaceAll(";.*", "");
                provider.setAccountChanged(false);
                return SMSActionResult.LOGIN_SUCCESSFUL();
            }

        }

        SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.login_error));
        smsActionResult.setRetryMakesSense(false);
        return smsActionResult;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException {
        return sendSMS(smsText, receivers, dateTime, spinnerText);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        return sendSMS(smsText, receivers, null, spinnerText);
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

        if (isLoginNeeded()) {
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
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

        String content = UrlConnectionFactory.inputStream2DebugString(new GZIPInputStream(inputStream), ENCODING);
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

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                return i;
            }
        }
        return WITH_PHONE_NUMBER;
    }
}
