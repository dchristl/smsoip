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

package de.christl.smsoip.supplier.directbox;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

public class DirectboxSupplier implements TimeShiftSupplier, ExtendedSMSSupplier {


    private DirectboxOptionProvider provider;

    //overwrite user agent, cause default is not valid for directbox
    private static final String TARGET_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)";
    private static final String ENCODING = "ISO-8859-1";

    private static final String LOGIN_URL = "https://directbox.com/portal/index.aspx";

    private static final String BALANCE_URL = "https://directbox.com/portal/services/WidgetService.asmx/WidgetLoad";
    private static final String NUMBER_AND_SEND_URL = "https://directbox.com/portal/sites/messaging/shortmessage.aspx";
    private static final String COOKIE_NAME = "ASP.NET_SessionId";
    private List<String> sessionId;
    private String sms;
    private String smsPrepaid;


    public DirectboxSupplier() {
        provider = new DirectboxOptionProvider(this);
    }

    public DirectboxSupplier(DirectboxOptionProvider provider) {
        this.provider = provider;
    }

    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        sessionId = new ArrayList<String>();
        StringBuilder bodyString = buildBodyString(userName, password);

        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        Map<String, String> requestProperties = new HashMap<String, String>(1);
        requestProperties.put("Content-Type", SendConstants.X_WWW_FORM_URL_ENCODING_WITH_UTF8);
        factory.setRequestProperties(requestProperties);
        factory.setTargetAgent(TARGET_AGENT);
        HttpURLConnection httpURLConnection = factory.writeBody(bodyString.toString());
        if (httpURLConnection == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        InputStream inputStream = httpURLConnection.getInputStream();
        if (headerFields == null || headerFields.size() == 0 || inputStream == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream, ENCODING);
        if (!response.contains("pageRedirect")) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpSessionId = UrlConnectionFactory.findCookieByName(headerFields, COOKIE_NAME.toUpperCase());
        if (tmpSessionId == null || tmpSessionId.equals("")) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        sessionId.add(tmpSessionId.replaceAll(";.*", ""));
        return SMSActionResult.LOGIN_SUCCESSFUL();
    }

    private StringBuilder buildBodyString(String userName, String password) throws IOException {
        StringBuilder bodyString = new StringBuilder();
        if (userName != null && !userName.contains("@")) {
            userName = userName + "@directbox.com";
        }

        //fetch the parameters for viewstate and eventvalidation
        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.create().getInputStream();
        if (inputStream == null) {
            throw new IOException("Network error");
        }
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        Elements viewstate = parse.select("input#__VIEWSTATE");
        String viewStateContent = viewstate.attr("value");
        viewStateContent = URLEncoder.encode(viewStateContent, ENCODING);
        Elements eventValidationElement = parse.select("input#__EVENTVALIDATION");
        String eventValidationContent = eventValidationElement.attr("value");
        eventValidationContent = URLEncoder.encode(eventValidationContent, ENCODING);

        bodyString.append(SendConstants.USER_NAME_FIELD_ID).append(URLEncoder.encode(userName == null ? "" : userName, ENCODING)).append("&");
        bodyString.append(SendConstants.PASSWORD_FIELD_ID).append(URLEncoder.encode(password == null ? "" : password, ENCODING)).append("&");
        bodyString.append(SendConstants.VIEWSTATE_LOGIN_PARAM).append(viewStateContent).append("&");
        bodyString.append(SendConstants.EVENT_VALIDATION_LOGIN__PARAM).append(eventValidationContent).append("&");
        bodyString.append(SendConstants.ASYNCPOST_PARAM).append("&");
        bodyString.append(SendConstants.LOGIN_BUTTON_PARAM);
        return bodyString;
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
        sms = null;
        smsPrepaid = null;
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_URL);
        Map<String, String> requestProperties = new HashMap<String, String>(1);
        requestProperties.put("Content-Type", SendConstants.JSON_ENCODING);
        factory.setRequestProperties(requestProperties);
        factory.setTargetAgent(TARGET_AGENT);
        factory.setCookies(sessionId);
        HttpURLConnection httpURLConnection = factory.writeBody(SendConstants.JSON_BALANCE_CONTENT);
        String result = UrlConnectionFactory.inputStream2DebugString(httpURLConnection.getInputStream(), ENCODING);
        return processBalanceReturn(result);
    }

    private SMSActionResult processBalanceReturn(String result) {
        //replace all known tags by html equivalent to make it Jsoup parsable
        result = result.replaceAll("\\\\u003c", "<");
        result = result.replaceAll("\\\\u003e", ">");
        result = result.replaceAll("\\\\u0027", "'");
        Document parse = Jsoup.parse(result);
        Elements smsContingent = parse.select("div[class*=smsContingent]");
        Elements prepaidContingent = parse.select("div[class*=smsPrepaid]");
        if (smsContingent == null || smsContingent.size() != 1 || prepaidContingent == null || prepaidContingent.size() != 2) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        String balance = smsContingent.text();
        smsPrepaid = prepaidContingent.get(0).text().replaceAll("\\\\n", "").trim();
        sms = prepaidContingent.get(1).text().replaceAll("\\\\n", "").trim();
        String balanceText = String.format(provider.getTextByResourceId(R.string.balance), balance, smsPrepaid, sms);
        return SMSActionResult.NO_ERROR(balanceText);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        return fireTimeShiftSMS(smsText, receivers, spinnerText, null);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException, NumberFormatException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            provider.saveState();
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        refreshInformations(true); //refresh the informations to get fresh data
        if (sms != null && smsPrepaid != null) {
            String tmpSMS = sms.replaceAll("\\D", "");
            String tmpSmsPrepaid = smsPrepaid.replaceAll("\\D", "");
            int availableSMS = 0;
            try {
                availableSMS = Integer.parseInt(tmpSMS) + Integer.parseInt(tmpSmsPrepaid);
            } catch (NumberFormatException ignored) {
                //ignore this and do the default behaviour
            }
            int smsCosts = receivers.size();
            if (provider.isSIActivated()) {
                smsCosts *= 2;
            }
            if (availableSMS < smsCosts) {
                provider.saveState();
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_balance)), receivers);
            }
        }

        //get the user dependent view and send state first
        UrlConnectionFactory factory = new UrlConnectionFactory(NUMBER_AND_SEND_URL, UrlConnectionFactory.METHOD_GET);
        factory.setTargetAgent(TARGET_AGENT);
        factory.setCookies(sessionId);
        HttpURLConnection con = factory.create();
        Document messagePage = Jsoup.parse(con.getInputStream(), ENCODING, "");
        String viewStateRawValue = messagePage.select("#__VIEWSTATE").attr("value");
        String eventValidationRawValue = messagePage.select("#__EVENTVALIDATION").attr("value");
        if (viewStateRawValue.equals("") || eventValidationRawValue.equals("")) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.NETWORK_ERROR(), receivers);
        }
        String viewStateContent = URLEncoder.encode(viewStateRawValue, ENCODING);
        String eventValidationContent = URLEncoder.encode(eventValidationRawValue, ENCODING);

        factory = new UrlConnectionFactory(NUMBER_AND_SEND_URL);
        Map<String, String> requestProperties = new HashMap<String, String>(1);
        requestProperties.put("Content-Type", SendConstants.X_WWW_FORM_URL_ENCODING);
        factory.setRequestProperties(requestProperties);
        factory.setTargetAgent(TARGET_AGENT);
        factory.setCookies(sessionId);

        StringBuilder bodyString = new StringBuilder();
        bodyString.append(SendConstants.VIEWSTATE_SEND_PARAM).append(viewStateContent).append("&");
        bodyString.append(SendConstants.EVENT_VALIDATION_SEND__PARAM).append(eventValidationContent).append("&");
        smsText = smsText.replaceAll("\\+", "%2B");
        smsText = smsText.replaceAll("%", "%25");

        bodyString.append(SendConstants.TEXT_FIELD_ID).append(URLDecoder.decode(smsText, ENCODING)).append("&");
        StringBuilder receiverString = new StringBuilder();
        for (Receiver receiver : receivers) {
            receiverString.append(receiver.getReceiverNumber()).append("%3B");
        }
        bodyString.append(SendConstants.TO_FIELD_ID).append(receiverString).append("&");


        String dateTimeString = "";  //15.10.2012+13%3A30
        if (dateTime != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy'+'HH'%3A'mm");
            dateTimeString = format.format(dateTime.getCalendar().getTime());
        }
        bodyString.append(SendConstants.TIME_FIELD_ID).append(dateTimeString).append("&");


        String fromString = "False";
        if (provider.isSIActivated()) {
            String sender = provider.getSender();
            if (sender == null) {
                provider.saveState();
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.refresh_sender_first)), receivers);
            } else {
                fromString = sender.replaceAll("^\\+", "00");
            }
        }

        bodyString.append(SendConstants.FROM_FIELD_ID).append(fromString).append("&");


        bodyString.append(SendConstants.SEND_BUTTON_PARAM);

        HttpURLConnection httpURLConnection = factory.writeBody(bodyString.toString());
        SMSActionResult smsActionResult = processSendReturn(httpURLConnection.getInputStream());
        if (!smsActionResult.isSuccess()) {
            provider.saveState();
        }
        return FireSMSResultList.getAllInOneResult(smsActionResult, receivers);
    }

    static SMSActionResult processSendReturn(InputStream resultStream) throws IOException {
        Document parse = Jsoup.parse(resultStream, "UTF-8", "");   //uses UTF-8 on website, but not for sending
        Elements select = parse.select("span#ctl00_ContentPlaceHolder_successMessage");
        if (select.size() == 1) {
            StringBuilder text = new StringBuilder(select.text());
            Elements numbers = parse.select("div#KnownRecipientListView span");
            for (Element number : numbers) {
                text.append("<br/>").append(number.text());
            }
            return SMSActionResult.NO_ERROR(text.toString());
        }
        StringBuilder errorText = new StringBuilder();
        Elements error = parse.select("span#ctl00_ContentPlaceHolder_lblErrorMessage");
        if (error.size() == 1) {
            errorText = new StringBuilder(error.text());
        } else {
            Elements errorElements = parse.select(".error");
            for (Element errorElement : errorElements) {
                if (!errorElement.attr("style").contains("display:none")) {
                    errorText.append(errorElement.text());
                }
            }
        }
        return errorText.capacity() == 0 ? SMSActionResult.UNKNOWN_ERROR() : SMSActionResult.UNKNOWN_ERROR(errorText.toString());
    }

    @Override
    public int getMinuteStepSize() {
        return 1;
    }

    @Override
    public int getDaysInFuture() {
        return 365;
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
        UrlConnectionFactory factory = new UrlConnectionFactory(NUMBER_AND_SEND_URL, UrlConnectionFactory.METHOD_GET);
        factory.setTargetAgent(TARGET_AGENT);
        factory.setCookies(sessionId);
        InputStream inputStream = factory.create().getInputStream();
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        Elements options = parse.select("select#ctl00_ContentPlaceHolder_cbxFrom option");
        if (options.size() < 2) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        List<String> numbers = new ArrayList<String>();
        for (Element option : options) {
            String value = option.attr("value");
            if (!value.equalsIgnoreCase("false")) {
                numbers.add(value);
            }
        }
        provider.saveNumbers(numbers);
        return SMSActionResult.NO_ERROR();
    }
}
