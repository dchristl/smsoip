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

package de.christl.smsoip.supplier.goodmails;

import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class GoodmailsSupplier implements ExtendedSMSSupplier {


    private static final String LOGIN_URL = "http://www.goodmails.de/index.php?action=login";
    public static final String HOME_PAGE = "http://www.goodmails.de/index.php?action=userfrontpage";
    private static final String TARGET_URL = "http://www.goodmails.de/sms.php?action=sendSMS";
    private String sessionCookie;
    private static final String ENCODING = "UTF-8";

    private boolean found = false;
    private OptionProvider provider;

    static final String NOT_ALLOWED_YET = "NOT ALLOWED YET"; ///special case on resend
    static final String MESSAGE_SENT_SUCCESSFUL = "Die SMS wurde erfolgreich verschickt.";

    private String lastSentType;


    public GoodmailsSupplier() {
        provider = new GoodmailsOptionProvider();
    }


    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        int sendIndex = findSendMethod(spinnerText);
        HttpURLConnection urlConn;
        InputStream is = null;
        String tmpUrl = TARGET_URL + "&sid=" + getSIDParamater();
        FireSMSResultList out = new FireSMSResultList();
        for (Receiver receiver : receivers) {
            StringBuilder builder = new StringBuilder();
            OutputStreamWriter writer = null;
            BufferedReader reader = null;
            try {
                URL myUrl = new URL(tmpUrl);
                urlConn = (HttpURLConnection) myUrl.openConnection();
                urlConn.setDoOutput(true);
                urlConn.setReadTimeout(TIMEOUT);
                urlConn.setConnectTimeout(TIMEOUT);
                urlConn.setRequestProperty("Cookie", sessionCookie);
                urlConn.setRequestProperty("User-Agent", ExtendedSMSSupplier.TARGET_AGENT);
                writer = new OutputStreamWriter(urlConn.getOutputStream());
                String headerFields = "&to=" + receiver.getReceiverNumber() + "&smsText=" + URLEncoder.encode(smsText, ENCODING);
                switch (sendIndex) {
                    case 0: //free
                        headerFields += "&type=freesms";
                        lastSentType = "FREE";
                        break;
                    case 1:  //standard
                        headerFields += "&type=standardsms";
                        lastSentType = "STANDARD";
                        break;
                    default:  //fake
                        headerFields += "&type=aksms";
                        lastSentType = "FAKE";
                        break;
                }
                writer.write(headerFields);
                writer.flush();
                is = urlConn.getInputStream();
                Map<String, List<String>> urlConnHeaderFields = urlConn.getHeaderFields();
                if (urlConnHeaderFields == null) {   //normally not reachable cause will be an IOException in getInputStream
                    out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
                    continue;
                }
                String line;
                reader = new BufferedReader(new InputStreamReader(is, ENCODING));

                while ((line = reader.readLine()) != null) {
                    builder.append(processLine(line));
                }
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
                continue;
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    Log.e(this.getClass().getCanonicalName(), "IOException", e);
                }
            }

            String alternateText = builder.toString();
            if (messageSuccessful(builder)) {
                if (alternateText.equals("NOT ALLOWED YET")) {
                    alternateText = provider.getTextByResourceId(R.string.text_alternate_not_allowed_yet);
                }
                SMSActionResult actionResult = SMSActionResult.UNKNOWN_ERROR();
                if (!alternateText.equals("")) {
                    actionResult = SMSActionResult.UNKNOWN_ERROR(alternateText);
                }
                out.add(new FireSMSResult(receiver, actionResult));
                continue;
            }
            out.add(new FireSMSResult(receiver, SMSActionResult.NO_ERROR(alternateText)));
        }

        return out;
    }

    private boolean messageSuccessful(StringBuilder builder) {
        return !builder.toString().equals(MESSAGE_SENT_SUCCESSFUL);
    }


    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() {
        return refreshInformations(true);
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() {
        return refreshInformations(false);
    }

    private SMSActionResult refreshInformations(boolean afterMessageSentSuccessful) {
        if (!afterMessageSentSuccessful) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        String tmpText = provider.getTextByResourceId(R.string.text_refresh_informations);
        HttpURLConnection urlConn;
        InputStream is = null;
        String credits = null;
        String tmpUrl = HOME_PAGE + "&sid=" + getSIDParamater();
        BufferedReader reader = null;
        try {
            URL myUrl = new URL(tmpUrl);
            urlConn = (HttpURLConnection) myUrl.openConnection();
            urlConn.setReadTimeout(ExtendedSMSSupplier.TIMEOUT);
            urlConn.setConnectTimeout(TIMEOUT);
            urlConn.setConnectTimeout(TIMEOUT);
            urlConn.setDoOutput(true);
            urlConn.setRequestProperty("Cookie", sessionCookie);
            urlConn.setRequestProperty("User-Agent", ExtendedSMSSupplier.TARGET_AGENT);
            is = urlConn.getInputStream();
            String line;
            reader = new BufferedReader(new InputStreamReader(is, ENCODING));
            while ((line = reader.readLine()) != null) {
                if (line.contains("name=\"glf_password\"")) {
                    Pattern p = Pattern.compile("value=\"[0-9]+[\\.+[0-9]+]*\"");
                    Matcher m = p.matcher(line);
                    while (m.find()) {
                        if (credits == null) {
                            credits = line.substring(m.start() + 1, m.end() - 1).replaceAll("[^0-9]", "");
                        }
                    }
                }
            }
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return SMSActionResult.NETWORK_ERROR();

        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
            }
        }

        return SMSActionResult.NO_ERROR(String.format(tmpText, credits));
    }

    private String getSIDParamater() {
        String out = sessionCookie.replaceAll("sessionSecret_", "");
        out = out.replaceAll("=.*", "");
        return out;
    }


    private String processLine(String s) {
        if (s.equals(NOT_ALLOWED_YET)) {
            return NOT_ALLOWED_YET;
        }
        String out = null;
        String returnClass = "<div id=\"sms-message-container\">";
        String end = "</div>";
        if (s.contains(returnClass)) {
            found = true;
            out = s.replaceAll(".*" + returnClass, "");
        }
        if (found) {
            if (s.contains(end)) {
                found = false;
                return out != null ? out.replaceAll(end + ".*", "") : s.replaceAll(end + ".*", "");
            }
            return s;

        }
        return "";
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) {
        String tmpUrl;
        try {
            tmpUrl = LOGIN_URL + "&glf_username=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&glf_password=" +
                    URLEncoder.encode(password == null ? "" : password, ENCODING) + "&email_domain=goodmails.de&language=deutsch&do=login";
        } catch (UnsupportedEncodingException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR();
        }
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(ExtendedSMSSupplier.TIMEOUT);
            con.setRequestProperty("User-Agent", ExtendedSMSSupplier.TARGET_AGENT);
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(false);
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null || headerFields.size() == 0) {
                return SMSActionResult.NETWORK_ERROR();
            }
            for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
                String cookie = stringListEntry.getKey();
                if (cookie != null && cookie.equalsIgnoreCase("set-cookie")) {
                    for (String s : stringListEntry.getValue()) {
                        if (s.startsWith("sessionSecret")) {
                            sessionCookie = s;
                            return SMSActionResult.LOGIN_SUCCESSFUL();
                        }
                    }
                }
            }
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return SMSActionResult.NETWORK_ERROR();
        }

        return SMSActionResult.LOGIN_FAILED_ERROR();
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                return i;
            }
        }
        return 0;
    }
}
