package de.christl.smsoip.supplier.freenet;

import android.text.Editable;
import android.text.Html;
import android.util.Log;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class for handling sms by freenet
 */
public class FreenetSupplier implements SMSSupplier {

    private FreenetOptionProvider provider;

    private final String LOGIN_URL = "https://auth.freenet.de/portal/login.php";
    private final String HOME_URL = "http://webmail.freenet.de/login/index.html";
    private final String TARGET_URL = "http://webmail.freenet.de/Sms/View/Send";
    private final String SEND_URL = "http://webmail.freenet.de/Sms/Action/Send?myAction=send&";
    private List<String> sessionCookies;
    private String ENCODING = "ISO-8859-1";

    public FreenetSupplier() {
        provider = new FreenetOptionProvider();
    }

    FreenetSupplier(FreenetOptionProvider provider) {
        this.provider = provider;
    }

    @Override
    public Result refreshInformationOnRefreshButtonPressed() {
        return refreshInformations(false);
    }

    @Override
    public Result refreshInformationAfterMessageSuccessfulSent() {
        return refreshInformations(true);
    }

    private Result refreshInformations(boolean noLoginBefore) {
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            Result result = login(provider.getUserName(), provider.getPassword());
            if (!result.equals(Result.NO_ERROR)) {
                return result;
            }
        }
        String tmpUrl = TARGET_URL;
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            con.setRequestProperty("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
            con.setRequestProperty("Accept-Encoding", "gzip, deflate");
            con.setRequestMethod("GET");
            StringBuilder cookieBuilder = new StringBuilder();
            for (String sessionCookie : sessionCookies) {
                cookieBuilder.append(sessionCookie).append(";");
            }
            con.setRequestProperty("Cookie", cookieBuilder.toString());
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
        String message = inputStream2String(is);
        message = message.replaceAll(".*SMSnotify\" value=\"", "");
        message = message.replaceAll("\">.*", "");
        message = Html.fromHtml(message).toString();
        if (message.contains("erfolgreich")) {
            return Result.NO_ERROR.setAlternateText(message);
        }

        return Result.UNKNOWN_ERROR.setAlternateText(message);
    }

    private String inputStream2String(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, ENCODING));
        String line;
        StringBuilder returnFromServer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            returnFromServer.append(line);
        }
        return returnFromServer.toString();
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
//    POST /portal/login.php HTTP/1.1
//    Host: auth.freenet.de
//    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0
//    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//    Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//    Accept-Encoding: gzip, deflate
//    Connection: keep-alive
//    Referer: http://www.freenet.de/index.html?status=log1&cbi=logMail
//    Content-Type: application/x-www-form-urlencoded
//    Content-Length: 216
//
//    cbi=logMail&callback=http%3A%2F%2Ftools.freenet.de%2Fmod_perl%2Flinker%2Ffreenet_startseite_loginkasten_mail%2Fwebmail.freenet.de%2Flogin%2Findex.html&username=USERNAME&passtext=Passwort&password=PASSWORD&x=0&y=0
    public Result login(String userName, String password) {
        sessionCookies = new ArrayList<String>();
        String tmpUrl = LOGIN_URL + "?username=" + userName + "&password=" + password;
        HttpURLConnection con;
        try {
            //first get the login cookie
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.LOGIN_FAILED_ERROR;
            }
            String sidCookie = null;
            Outer:
            for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
                String cookieList = stringListEntry.getKey();
                if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                    for (String cookie : stringListEntry.getValue()) {
                        if (cookie.startsWith("SID")) {
                            sessionCookies.add(cookie);
                            sidCookie = cookie;
                            break Outer;
                        }
                    }
                }
            }
            if (sidCookie == null) {
                return Result.LOGIN_FAILED_ERROR;
            }
            //now get the freenetMail4Prev cookie, that is also needed
            con = (HttpURLConnection) new URL(HOME_URL).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            con.setRequestProperty("Cookie", sidCookie);
            headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.LOGIN_FAILED_ERROR;
            }
            Outer:
            for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
                String cookieList = stringListEntry.getKey();
                if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                    for (String cookie : stringListEntry.getValue()) {
                        if (cookie.startsWith("freenetMail4Prev")) {
                            sessionCookies.add(cookie);
                            break Outer;
                        }
                    }
                }
            }
            if (sessionCookies.size() != 2) {
                return Result.LOGIN_FAILED_ERROR;
            }
            return Result.NO_ERROR;
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;
        }


    }

    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        return fireSMS(smsText.toString(), receivers.get(0).toString());
    }

    //    myAction=send&from=b<USERNAME>&senderName=service%40freenet.de&defaultEmailSender=&to=<NUMBER>&smsText=Test+123+%F6+%FC+%E4+%DF
    public Result fireSMS(String smsText, String receiver) {
        Result result = login(provider.getUserName(), provider.getPassword());
        if (!result.equals(Result.NO_ERROR)) {
            return result;
        }
        try {
            smsText = URLEncoder.encode(smsText, ENCODING);
        } catch (UnsupportedEncodingException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }
        String tmpUrl = SEND_URL + "&senderName=service%40freenet.de&defaultEmailSender=&to=" + receiver + "&smsText=" + smsText;
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            StringBuilder cookieBuilder = new StringBuilder();
            for (String sessionCookie : sessionCookies) {
                cookieBuilder.append(sessionCookie).append(";");
            }
            con.setRequestProperty("Cookie", cookieBuilder.toString());
            return processReturn(con.getInputStream());
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR;
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR;
        }
    }
}
