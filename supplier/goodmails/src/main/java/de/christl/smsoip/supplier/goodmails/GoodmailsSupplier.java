package de.christl.smsoip.supplier.goodmails;

import android.text.Editable;
import android.util.Log;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;
import de.christl.smsoip.supplier.goodmails.constant.Constants;

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
public class GoodmailsSupplier implements SMSSupplier {


    private static final String LOGIN_URL = "http://www.goodmails.de/index.php?action=login";
    public static final String HOME_PAGE = "http://www.goodmails.de/index.php?action=userfrontpage";
    private static final String TARGET_URL = "http://www.goodmails.de/sms.php?action=sendSMS";
    private String sessionCookie;
    private String encoding = "ISO-8859-1";

    private boolean found = false;
    OptionProvider provider;

    static final String NOT_ALLOWED_YET = "NOT ALLOWED YET"; ///special case on resend
    static final String MESSAGE_SENT_SUCCESSFUL = "Die SMS wurde erfolgreich verschickt.";

    private String lastSentType;


    public GoodmailsSupplier() {
        provider = new GoodmailsOptionProvider();
    }


    //    POST /sms.php?action=sendSMS&sid=9jno2qh83lt1nl8md76p2c6ns6 HTTP/1.1
//    Host: www.goodmails.de
//    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0
//    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//    Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//    Accept-Encoding: gzip, deflate
//    Proxy-Connection: keep-alive
//    Referer: http://www.goodmails.de/sms.php?sid=9jno2qh83lt1nl8md76p2c6ns6&action=compose&type=2
//    Cookie: sessionSecret_39pl3t9p8bgt0ip990buos3jr5=681e359d8aec81c0dfd270dc2b3eb7d8; sessionSecret_cqjink2rqid28h21rfamo3jhe2=c2eccf2c5b9436b670cbf383d7a0922b; sessionSecret_9jno2qh83lt1nl8md76p2c6ns6=2bb048c513937f5a5417ec83a90977de
//    Content-Type: application/x-www-form-urlencoded
//    Content-Length: 109
//
//    type=standardsms&to=454564654&smsText=bla+bla+blubbsklyajdjkshdhsdhjksd%0D%0A%0D%0Asadasdsd%0D%0Af%0D%0Asdf

    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        Result result = login(provider.getUserName(), provider.getPassword());
        if (!result.equals(Result.NO_ERROR)) {
            return result;
        }
        HttpURLConnection urlConn;
        InputStream is = null;
        String tmpUrl = TARGET_URL + "&sid=" + getSIDParamater();
        StringBuilder builder = new StringBuilder();
        try {
            URL myUrl = new URL(tmpUrl);
            urlConn = (HttpURLConnection) myUrl.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setReadTimeout(SMSSupplier.TIMEOUT);
            urlConn.setRequestProperty("Cookie", sessionCookie);
            urlConn.setRequestProperty("User-Agent", SMSSupplier.TARGET_AGENT);
            OutputStreamWriter writer = new OutputStreamWriter(urlConn.getOutputStream());
            String headerFields = "&to=" + receivers.get(0) + "&smsText=" + URLEncoder.encode(smsText.toString(), encoding);
            if (spinnerText.equals(Constants.FREE)) {
                headerFields += "&type=freesms";
                lastSentType = "FREE";
            } else if (spinnerText.equals(Constants.STANDARD)) {
                headerFields += "&type=standardsms";
                lastSentType = "STANDARD";
            } else if (spinnerText.equals(Constants.FAKE)) {
                headerFields += "&type=aksms";
                lastSentType = "FAKE";
            }
            writer.write(headerFields);
            writer.flush();
            is = urlConn.getInputStream();
            Outer:
            for (Map.Entry<String, List<String>> header : urlConn.getHeaderFields().entrySet()) {
                if (header.getKey().equals("content-type")) {
                    for (String entry : header.getValue()) {
                        String charset = "charset";
                        if (entry.contains(charset)) {
                            encoding = entry.substring(entry.indexOf(charset) + charset.length() + 1);
                            break Outer;
                        }
                    }
                }
            }
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));

            while ((line = reader.readLine()) != null) {
                builder.append(processLine(line));
            }
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
            }
        }
        String alternateText = builder.toString();
        if (messageSuccessful(builder)) {
            if (alternateText.equals("NOT ALLOWED YET")) {
                alternateText = provider.getTextByResourceId(R.string.text_alternate_not_allowed_yet);
            }
            return Result.UNKNOWN_ERROR.setAlternateText(alternateText);
        }
        return Result.NO_ERROR.setAlternateText(alternateText);
    }

    private boolean messageSuccessful(StringBuilder builder) {
        return !builder.toString().equals(MESSAGE_SENT_SUCCESSFUL);
    }


    @Override
    public Result refreshInformationOnRefreshButtonPressed() {
        return refreshInformations(false);
    }

    @Override
    public Result refreshInformationAfterMessageSuccessfulSent() {
        return refreshInformations(true);
    }

    private Result refreshInformations(boolean afterMessageSentSuccessful) {
        if (!afterMessageSentSuccessful) {   //dont do a extra login if message is sent short time before
            Result result = login(provider.getUserName(), provider.getPassword());
            if (!result.equals(Result.NO_ERROR)) {
                return result;
            }
        }
        String tmpText = "Credits: %s";
        HttpURLConnection urlConn;
        InputStream is = null;
        String credits = null;
        String tmpUrl = HOME_PAGE + "&sid=" + getSIDParamater();
        try {
            URL myUrl = new URL(tmpUrl);
            urlConn = (HttpURLConnection) myUrl.openConnection();
            urlConn.setReadTimeout(SMSSupplier.TIMEOUT);
            urlConn.setDoOutput(true);
            urlConn.setRequestProperty("Cookie", sessionCookie);
            urlConn.setRequestProperty("User-Agent", SMSSupplier.TARGET_AGENT);
            is = urlConn.getInputStream();
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
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
            return Result.TIMEOUT_ERROR;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
            }
        }

        return Result.NO_ERROR.setAlternateText(String.format(tmpText, credits));
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
    public String getProviderInfo() {
        return "GM -> " + lastSentType;
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public Result login(String userName, String password) {
        String tmpUrl = LOGIN_URL + "&glf_username=" + userName + "&glf_password=" +
                password + "&email_domain=goodmails.de&language=deutsch&do=login";
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(SMSSupplier.TIMEOUT);
            con.setRequestProperty("User-Agent", SMSSupplier.TARGET_AGENT);
            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(false);
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null || headerFields.size() == 0) {
                return Result.NETWORK_ERROR;
            }
            for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
                String cookie = stringListEntry.getKey();
                if (cookie.equalsIgnoreCase("set-cookie")) {
                    for (String s : stringListEntry.getValue()) {
                        if (s.startsWith("sessionSecret")) {
                            sessionCookie = s;
                            return Result.NO_ERROR;
                        }
                    }
                }
            }
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;
        }

        return Result.LOGIN_FAILED_ERROR;
    }
}
