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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class GMXSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private GMXOptionProvider provider;
    /**
     * this.getClass().getCanonicalName() for output.
     */


    private static final String TARGET_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1-1.IBehaviorListener.0-main~tab-content~panel~container-content~panel-form-sendMessage&wicket-ajax=true&wicket-ajax-baseurl=home";
    private static final String SAVE_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1-1.IBehaviorListener.0-main~tab-content~panel~container-content~panel-form-send~date~panel-send~date~form-save&wicket-ajax=true&wicket-ajax-baseurl=home";

    private static final String FIND_NUMBERS_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1";


    private static final String LOGIN_URL = "https://ums.gmx.net/ums/login?0-1.IFormSubmitListener-login&dev=dsk";
    private String sessionId;
    private static final String SESSION_ID_URL_STRING = "jsessionid";
    private Document lastParsedDocument;
    private String sendNowRadioButtonId;
    private String sendLaterRadioButtonId;
    private static final String CRLF = "\r\n";
    private static final String ENCODING = "UTF-8";

    public static final int WITH_PHONE_NUMBER = 0;
    public static final int WITH_FREE_TEXT = 1;

    private Long leaseTime;

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

        String tmpUrl = String.format(TARGET_URL, sessionId);
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        factory.writeMultipartBody(parameterMap, ENCODING);
        HttpURLConnection connnection = factory.getConnnection();
        SMSActionResult result = processReturn(connnection.getInputStream());
        if (result.isSuccess()) {
            provider.saveLastSender(sendMethod);
        } else {
            provider.saveTemporaryState();
        }
        return FireSMSResultList.getAllInOneResult(result, receivers);

    }

    private SMSActionResult sendSaveRequest(Map<String, String> parameterMap) {

        Map<String, String> otherParameterMap = new HashMap<String, String>(parameterMap);
        otherParameterMap.put("send-date-panel:send-date-form:save", "1");
        try {
            String tmpUrl = String.format(SAVE_URL, sessionId);
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
        if (lastParsedDocument == null) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        String infoText = findInfoText();
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
     * <div id="SMMS_tab_content_info">
     * <div id="SMMS_tab_content_info_status">8. Juni 2012
     * </div>
     * <div id="SMMS_tab_content_info_free">
     * <div class="SMMS_tab_content_info_text">Frei<span>SMS</span>
     * </div>
     * <div class="SMMS_tab_content_info_text_small">noch 3
     * von 10
     * </div>
     * </div>
     * <div id="SMMS_tab_content_info_pay">
     * <div class="SMMS_tab_content_info_text">
     * Pay<span>SMS</span></div>
     * <div class="SMMS_tab_content_info_text_small">0
     * versendet
     * </div>
     * </div>
     * </div>
     *
     * @return found info string or empty if not found
     * @throws IOException
     */
    private String findInfoText() throws IOException {
        StringBuilder out = new StringBuilder("");
        Elements freeElementText = lastParsedDocument.select("#SMMS_tab_content_info_free").select(".SMMS_tab_content_info_text");
        if (freeElementText.size() > 0) {
            out.append(freeElementText.text());
            out.append(" : ");
        }
        Elements freeElement = lastParsedDocument.select("#SMMS_tab_content_info_free").select(".SMMS_tab_content_info_text_small");
        if (freeElement.size() > 0) {
            out.append(freeElement.text());
            out.append(CRLF);
        }
        Elements payElementText = lastParsedDocument.select("#SMMS_tab_content_info_pay").select(".SMMS_tab_content_info_text");
        if (payElementText.size() > 0) {
            out.append(payElementText.text());
            out.append(" : ");
        }
        Elements payElement = lastParsedDocument.select("#SMMS_tab_content_info_pay").select(".SMMS_tab_content_info_text_small");
        if (payElement.size() > 0) {
            out.append(payElement.text());
        }
        return out.toString();
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException {
        String tmpUrl;
        leaseTime = null;
        tmpUrl = LOGIN_URL + "&login_form_hf_0=&token=false&email=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&password=" + URLEncoder.encode(password == null ? "" : password, ENCODING);
        sendNowRadioButtonId = null;
        sendLaterRadioButtonId = null;
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        sessionId = null;
        HttpURLConnection con;
        String inputStream;
        con = factory.create();

        //no network
        Map<String, List<String>> headerFields = con.getHeaderFields();
        if (headerFields == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        InputStream tmpStream = con.getInputStream();
        if (tmpStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        inputStream = UrlConnectionFactory.inputStream2DebugString(tmpStream);
        Document document = Jsoup.parse(inputStream);
        Elements scripts = document.select("script");
        for (Element script : scripts) {
            String data = script.data();
            if (data.contains(SESSION_ID_URL_STRING)) {
                sessionId = data.replaceAll("\\s", "");
                sessionId = sessionId.replaceAll(".*jsessionid=", "");
                sessionId = sessionId.replaceAll("\\?.*", "");
                break;
            }
        }

        Elements radioButtons = document.select("input[id^=SMMS_tab_content_radio]");
        for (Element radioButton : radioButtons) {
            if (radioButton.hasAttr("checked")) {
                sendNowRadioButtonId = radioButton.attr("value");
            } else {
                sendLaterRadioButtonId = radioButton.attr("value");
            }
        }
        if (!(sessionId == null || sessionId.length() == 0)) {
            lastParsedDocument = document;
            leaseTime = System.currentTimeMillis();
            provider.setAccountChanged(false);
            return SMSActionResult.LOGIN_SUCCESSFUL();
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

        String tmpUrl = String.format(FIND_NUMBERS_URL, sessionId);

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.getConnnection().getInputStream();
        Document document = Jsoup.parse(inputStream, ENCODING, "");
        Elements select = document.select("select#SMMS_tab_number_selector_drop option");
        if (select.size() < 1) {
            return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_numbers_maintened));
        }
        HashMap<Integer, String> numbers = new HashMap<Integer, String>(select.size());
        try {
            for (Element element : select) {
                int id = Integer.parseInt(element.attr("value"));
                numbers.put(id, element.text());
            }
            provider.saveNumbers(numbers);
        } catch (NumberFormatException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
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
