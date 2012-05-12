package de.christl.smsoip.supplier.gmx;

import android.text.Editable;
import android.util.Log;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class GMXSupplier implements SMSSupplier {

    OptionProvider provider;
    /**
     * this.getClass().getCanonicalName() for output.
     */


    private static final String TARGET_URL = "https://ums.gmx.net/ums/home;jsessionid=%s?1-1.IBehaviorListener.0-main~tab-content~panel~container-content~panel-form-sendMessage&wicket-ajax=true&wicket-ajax-baseurl=home";

    private static final String LOGIN_URL = "https://ums.gmx.net/ums/login?0-1.IFormSubmitListener-login&dev=dsk";
    private String sessionId;
    private static final String SESSION_ID_URL_STRING = "jsessionid";
    private DataInputStream lastInputStream;
    private String genericRadioButtonId;
    private static final String CRLF = "\r\n";
    private final static String ENCODING = "UTF-8";

    public GMXSupplier() {
        provider = new GMXOptionProvider();
    }


    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        Result result = login(provider.getUserName(), provider.getPassword());
        if (!result.equals(Result.NO_ERROR)) {
            return result;
        }
        if (provider.getSettings().getBoolean(GMXOptionProvider.PROVIDER_CHECKNOFREESMSAVAILABLE, false)) {
            Result tmpResult = refreshInformations(true, 0);
            if (tmpResult.equals(Result.NO_ERROR)) {
                String userText = tmpResult.getUserText().toString();
                String[] split = userText.split(" ");
                boolean noFreeAvailable;
                if (split.length > 1) {
                    int freeSMS;
                    try {
                        freeSMS = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {
                        return Result.UNKNOWN_ERROR.setAlternateText(provider.getTextByResourceId(R.string.text_free_messages_could_not_resolved));
                    }
                    int messageLength = getProvider().getTextMessageLength();
                    int smsCount = Math.round((smsText.toString().length() / messageLength));
                    smsCount = smsText.toString().length() % messageLength == 0 ? smsCount : smsCount + 1;
                    noFreeAvailable = !((receivers.size() * smsCount) <= freeSMS);
                } else {
                    return Result.UNKNOWN_ERROR.setAlternateText(provider.getTextByResourceId(R.string.text_free_messages_could_not_resolved));
                }
                if (noFreeAvailable) {
                    return Result.UNKNOWN_ERROR.setAlternateText(provider.getTextByResourceId(R.string.text_no_free_messages_available));
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
            Editable receiver = receivers.get(i);
            receiverListBuilder.append(String.format("{;;%s}", receiver));
            if (i + 1 != receivers.size()) {
                receiverListBuilder.append(",");
            }
        }
        parameterMap.put("recipients", receiverListBuilder.toString());
        parameterMap.put("upload-panel:upload-form:file\"; filename=\"", "");
        parameterMap.put("subject", "");
        parameterMap.put("textMessage", smsText.toString());
        parameterMap.put("send-date-panel:send-date-form:send-options-rdgrp", genericRadioButtonId);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        parameterMap.put("send-date-panel:send-date-form:send-date", sdf.format(new Date()));
        parameterMap.put("send-date-panel:send-date-form:send-date-hour", "0");
        parameterMap.put("send-date-panel:send-date-form:send-date-minute", "0");
        parameterMap.put("sendMessage", "1");
        HttpURLConnection con;

        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setDoOutput(true);
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.setRequestProperty("Cookie", "dev=dsk");
            OutputStream output = con.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, ENCODING), true);
            for (Map.Entry<String, String> stringStringEntry : parameterMap.entrySet()) {
                writer.append("--").append(boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"").append(stringStringEntry.getKey()).append("\"").append(CRLF);
                writer.append(CRLF);
                writer.append(stringStringEntry.getValue()).append(CRLF).flush();

            }
            writer.append("--").append(boundary).append("--").append(CRLF).flush();
            return processReturn(con.getInputStream());


        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;
        }
    }

    private Result processReturn(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, ENCODING));


        String line;
        StringBuilder returnFromServer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            returnFromServer.append(line);
        }
        String message = returnFromServer.toString();
        if (message.contains("feedbackPanelERROR")) {
            message = message.replaceAll(".*<span class=\"feedbackPanelERROR\">", "");
            message = message.replaceAll("<.*", "");
            //some additional clean up
            message = message.replaceAll("[\\[\\]^]", "");
            return Result.UNKNOWN_ERROR.setAlternateText(message);
        } else if (message.contains("confirmation_message_text")) {
            message = message.replaceAll(".*<div id=\"confirmation_message_text\">", "");
            message = message.replaceAll("<.*", "");
            return Result.NO_ERROR.setAlternateText(message);
        }

        return Result.UNKNOWN_ERROR.setAlternateText(message);
    }

    @Override
    public Result refreshInformationOnRefreshButtonPressed() {
        Result result = refreshInformations(false, 0);
        if (result.equals(Result.NO_ERROR) && result.getUserText().equals("")) {              //informations are not available at first try so do it twice
            result = refreshInformations(false, 0);
        }
        return result;

    }

    @Override
    public Result refreshInformationAfterMessageSuccessfulSent() {
        return refreshInformations(true, 0);
    }

    private Result refreshInformations(boolean noLoginBefore, int tryNr) {
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            Result result = login(provider.getUserName(), provider.getPassword());
            if (!result.equals(Result.NO_ERROR)) {
                return result;
            }
        }
        String infoText = "";
        if (lastInputStream == null) {
            return Result.UNKNOWN_ERROR;
        }
        String inputLine;
        try {
            while ((inputLine = lastInputStream.readLine()) != null) {
                if (inputLine.contains("<div class=\"SMMS_tab_content_info_text_small\">")) {
                    String tmp = inputLine.replaceAll(".*<div class=\"SMMS_tab_content_info_text_small\">", "");
                    infoText = tmp.replaceAll("</div>.*", "");
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }
        if (infoText.equals("") && tryNr < 5) {
            return refreshInformations(false, ++tryNr);
        }
        return Result.NO_ERROR.setAlternateText(infoText);
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
    public Result login(String userName, String password) {
        String tmpUrl = LOGIN_URL + "&login_form_hf_0=&token=false&email=" + userName + "&password=" + password;
        HttpURLConnection con;
        sessionId = null;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;
        }
        //no network
        Map<String, List<String>> headerFields = con.getHeaderFields();
        if (headerFields == null) {
            return Result.LOGIN_FAILED_ERROR;
        }
        try {
            lastInputStream = new DataInputStream(con.getInputStream());
            String inputLine;
            while ((inputLine = lastInputStream.readLine()) != null) {
                if (inputLine.contains(SESSION_ID_URL_STRING)) {
                    sessionId = inputLine.replaceAll(".*jsessionid=", "");
                    sessionId = sessionId.replaceAll("\\?.*", "");
                }
                if (inputLine.contains("SMMS_tab_content_radio") && inputLine.contains("checked")) {
                    genericRadioButtonId = inputLine.replaceAll(".*value=\"", "");
                    genericRadioButtonId = genericRadioButtonId.replaceAll("\" checked.*", "");
                }
                if (sessionId != null && sessionId.length() > 0 && genericRadioButtonId != null && genericRadioButtonId.length() > 0) {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }

        if (!(sessionId == null || sessionId.length() == 0)) {
            return Result.NO_ERROR;
        }
        return Result.LOGIN_FAILED_ERROR;
    }
}
