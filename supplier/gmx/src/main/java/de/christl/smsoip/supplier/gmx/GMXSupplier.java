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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class GMXSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private OptionProvider provider;
    /**
     * this.getClass().getCanonicalName() for output.
     */


    private static final String TARGET_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1-1.IBehaviorListener.0-main~tab-content~panel~container-content~panel-form-sendMessage&wicket-ajax=true&wicket-ajax-baseurl=home";

    private static final String LOGIN_URL = "https://ums.gmx.net/ums/login?0-1.IFormSubmitListener-login&dev=dsk";
    private String sessionId;
    private static final String SESSION_ID_URL_STRING = "jsessionid";
    private Document lastParsedDocument;
    private String sendNowRadioButtonId;
    private String sendLaterRadioButtonId;
    private static final String CRLF = "\r\n";
    private static final String ENCODING = "UTF-8";

    public GMXSupplier() {
        provider = new GMXOptionProvider();
    }

    public GMXSupplier(GMXOptionProvider provider) {
        this.provider = provider;
    }


    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTimeObject) {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        if (provider.getSettings().getBoolean(GMXOptionProvider.PROVIDER_CHECKNOFREESMSAVAILABLE, false)) {
            SMSActionResult tmpResult = refreshInformations(true, 0);
            if (tmpResult.isSuccess()) {
                String userText = tmpResult.getMessage();
                String[] split = userText.split(" ");
                boolean noFreeAvailable;
                if (split.length > 4) {
                    int freeSMS;
                    try {
                        freeSMS = Integer.parseInt(split[4]);
                    } catch (NumberFormatException e) {
                        return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_free_messages_could_not_resolved)), receivers);
                    }
                    int messageLength = getProvider().getTextMessageLength();
                    int smsCount = Math.round((smsText.length() / messageLength));
                    smsCount = smsText.length() % messageLength == 0 ? smsCount : smsCount + 1;
                    noFreeAvailable = !((receivers.size() * smsCount) <= freeSMS);
                } else {
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_free_messages_could_not_resolved)), receivers);
                }
                if (noFreeAvailable) {
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_no_free_messages_available)), receivers);
                }
            }
        }
        String tmpUrl = String.format(TARGET_URL, sessionId);

        String boundary = "--" + Long.toHexString(System.currentTimeMillis());
        Map<String, String> parameterMap = new LinkedHashMap<String, String>();
        parameterMap.put("id8_hf_0", "");
        parameterMap.put("from", "0");
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
        }
        parameterMap.put("send-date-panel:send-date-form:send-options-rdgrp", radioButtonId);
        parameterMap.put("send-date-panel:send-date-form:send-date", dateString);
        parameterMap.put("send-date-panel:send-date-form:send-date-hour", hour);
        parameterMap.put("send-date-panel:send-date-form:send-date-minute", minute);
        parameterMap.put("sendMessage", "1");
        HttpURLConnection con;

        PrintWriter writer = null;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setDoOutput(true);
            con.setReadTimeout(TIMEOUT);
            con.setConnectTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("Cookie", "dev=dsk");      //have to be called earlier
            OutputStream output = con.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(output, ENCODING), true);
            for (Map.Entry<String, String> stringStringEntry : parameterMap.entrySet()) {
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"").append(stringStringEntry.getKey()).append("\"").append(CRLF);
                writer.append(CRLF);
                writer.append(stringStringEntry.getValue()).append(CRLF).flush();

            }
            writer.append("--").append(boundary).append("--").append(CRLF).flush();
            return FireSMSResultList.getAllInOneResult(processReturn(con.getInputStream()), receivers);

        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return FireSMSResultList.getAllInOneResult(SMSActionResult.TIMEOUT_ERROR(), receivers);
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return FireSMSResultList.getAllInOneResult(SMSActionResult.NETWORK_ERROR(), receivers);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * its an ajax response and easier to handle than with JSoup
     *
     * @param is
     * @return
     * @throws IOException
     */
    SMSActionResult processReturn(InputStream is) throws IOException {

        Document document = Jsoup.parse(is, ENCODING, "");
        String feedBackPanelText = document.select("component").text();

        Document feedBackPanelContent = Jsoup.parse(feedBackPanelText, ENCODING);
        Elements feedBackPanelDiv = feedBackPanelContent.select("div");
        if (feedBackPanelDiv.select("span").attr("class").equals("feedbackPanelERROR")) {
            return SMSActionResult.UNKNOWN_ERROR(feedBackPanelDiv.text());
        } else {
            return SMSActionResult.NO_ERROR(feedBackPanelDiv.select("#confirmation_message_text").text());
        }
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() {
        SMSActionResult result = refreshInformations(false, 0);
        if (result.isSuccess() && result.getMessage().equals("")) {              //informations are not available at first try so do it twice
            result = refreshInformations(false, 0);
        }
        return result;

    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() {
        return refreshInformations(true, 0);
    }

    private SMSActionResult refreshInformations(boolean noLoginBefore, int tryNr) {
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        String infoText = "";
        if (lastParsedDocument == null) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        try {
            infoText = findInfoText();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }
//        if (infoText.equals("") && tryNr < 5) {
//            return refreshInformations(false, ++tryNr);
//        }
        return SMSActionResult.NO_ERROR(infoText);
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
    public String getProviderInfo() {
        return provider.getProviderName();
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) {
        String tmpUrl;
        try {
            tmpUrl = LOGIN_URL + "&login_form_hf_0=&token=false&email=" + URLEncoder.encode(userName, ENCODING) + "&password=" + URLEncoder.encode(password, ENCODING);
        } catch (UnsupportedEncodingException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        sendNowRadioButtonId = null;
        sendLaterRadioButtonId = null;
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        sessionId = null;
        HttpURLConnection con;
        try {
            con = factory.create();
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return SMSActionResult.NETWORK_ERROR();
        }
        //no network
        Map<String, List<String>> headerFields = con.getHeaderFields();
        if (headerFields == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        try {
            Document document = Jsoup.parse(con.getInputStream(), ENCODING, "");
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
                return SMSActionResult.LOGIN_SUCCESSFUL();
            }
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }


        return SMSActionResult.LOGIN_FAILED_ERROR();
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) {
        return fireSMS(smsText, receivers, spinnerText, dateTime);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) {
        return fireSMS(smsText, receivers, spinnerText, null);
    }

    @Override
    public int getMinuteStepSize() {
        return 5;
    }

    @Override
    public int getDaysInFuture() {
        return 365 * 5; //five years should be enough
    }
}
