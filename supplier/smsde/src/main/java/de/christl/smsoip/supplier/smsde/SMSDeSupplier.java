package de.christl.smsoip.supplier.smsde;


import android.text.Editable;
import android.util.Log;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SMSDeSupplier implements SMSSupplier {


    private SMSDeOptionProvider provider;

    private static final String LOGIN_FIRST_STEP_URL = "http://www.sms.de";
    private static final String LOGIN_SECOND_STEP_URL = "http://www.sms.de/login/refused.php";
    private static final String HOME_PAGE = "http://www.sms.de/index.php";
    private List<String> sessionCookies;
    private static final String ENCODING = "UTF-8";

    public SMSDeSupplier() {
        provider = new SMSDeOptionProvider();
    }

    public SMSDeSupplier(SMSDeOptionProvider provider) {
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

    private Result refreshInformations(boolean afterMessageSentSuccessful) {
        if (!afterMessageSentSuccessful) {   //dont do a extra login if message is sent short time before
            Result result = login(provider.getUserName(), provider.getPassword());
            if (!result.equals(Result.NO_ERROR())) {
                return result;
            }
        }
        try {
            //first get the login cookie
            HttpURLConnection con = (HttpURLConnection) new URL(HOME_PAGE).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("GET");
            StringBuilder cookieBuilder = new StringBuilder();
            for (int i = 0, sessionCookiesSize = sessionCookies.size(); i < sessionCookiesSize; i++) {
                String sessionCookie = sessionCookies.get(i);
                cookieBuilder.append(sessionCookie).append(i + 1 < sessionCookiesSize ? "; " : "");
            }
            con.setRequestProperty("Cookie", cookieBuilder.toString());
            return processRefreshInformations(con.getInputStream());
        } catch (SocketTimeoutException stoe)

        {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();

        } /*finally

            {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
            }
        }*/

//        return Result.NO_ERROR/*.setAlternateText(String.format(tmpText, credits))*/;
    }

    private Result processRefreshInformations(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, ENCODING));
        String line;
        String credits = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains("farb") && line.contains("/konto/index.php")) {
                credits = line.replaceAll(".*\">", "");
                credits = credits.replaceAll("<.*", "");
                credits = credits.replaceAll("[[^0-9]&&[^A-z]&&[^ ]].*", "");
                break;
            }
        }
        if (credits != null) {
            return Result.NO_ERROR().setAlternateText(credits);
        } else {
            return Result.UNKNOWN_ERROR();
        }
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

//FIRST STEP
//    HTTP/1.1 200 OKDate: Sat, 19 May 2012 10:08:55 GMTServer: Apache/2.2.3 (Debian) PHP/5.2.0-8+etch15X-Powered-By: PHP/5.2.0-8+etch15Set-Cookie: C_SMSDE_IDd620b85590=4a6dcfe622c6a43caa8506b495164ab6; path=/; domain=.sms.deSet-Cookie: sms=4a6dcfe622c6a43caa8506b495164ab6; path=/Expires: Thu, 19 Nov 1981 08:52:00 GMTCache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0Pragma: no-cacheContent-Type: text/html; charset=iso-8859-1Content-Length: 44798

    //SECOND STEP
//    POST /login/refused.php HTTP/1.1
//    Host: www.sms.de
//    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0
//    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//    Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//    Accept-Encoding: gzip, deflate
//    DNT: 1
//    Proxy-Connection: keep-alive
//    Referer: http://www.sms.de/
//    Cookie: C_SMSDE_IDd620b85590=90ac43bcf4525095d2e3daec6c854750
//    Content-Type: application/x-www-form-urlencoded
//    Content-Length: 34
//
//    username=<USERNAME>&passwd=<PASSWORD>
    @Override
    public Result login(String userName, String password) {
        //FIRST STEP
        sessionCookies = new ArrayList<String>();
        try {
            //first get the login cookie
            HttpURLConnection con = (HttpURLConnection) new URL(LOGIN_FIRST_STEP_URL).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("GET");
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.NETWORK_ERROR();
            }
            String FIRST_COOKIE = "C_SMSDE_ID";
            Outer:
            for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
                String cookieList = stringListEntry.getKey();
                if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                    for (String cookie : stringListEntry.getValue()) {
                        if (cookie.toUpperCase().startsWith(FIRST_COOKIE)) {
                            sessionCookies.add(cookie.replaceAll(";.*", ""));
                            break Outer;
                        }
                    }
                }
            }
            if (sessionCookies.size() != 1) {
                return Result.NETWORK_ERROR(); //not possible if network available
            }
            //now we have the login idependent id cookie
            con = (HttpURLConnection) new URL(LOGIN_SECOND_STEP_URL).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            StringBuilder cookieBuilder = new StringBuilder();
            for (String sessionCookie : sessionCookies) {
                cookieBuilder.append(sessionCookie);
            }
            con.setRequestProperty("Cookie", cookieBuilder.toString());
            con.setDoOutput(true);
            con.setInstanceFollowRedirects(false);
            OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
            String userNamePasswordBody = "username=" + userName + "&passwd=" + password;
            writer.write(userNamePasswordBody);
            writer.flush();
            headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.LOGIN_FAILED_ERROR();
            }
            //get the login cookie
            String tmpSessionCookie = sessionCookies.get(0);
            tmpSessionCookie = tmpSessionCookie.replaceAll("=.*", "");
            sessionCookies = new ArrayList<String>();
            for (Map.Entry<String, List<String>> stringListEntry : headerFields.entrySet()) {
                String cookieList = stringListEntry.getKey();
                if (cookieList != null && cookieList.equalsIgnoreCase("set-cookie")) {
                    for (String cookie : stringListEntry.getValue()) {
                        String c_smsde_uid = "C_SMSDE_UID=";
                        if (cookie.toUpperCase().startsWith(c_smsde_uid)) {  //the old cokie will be replaced by new one if succesfull
                            sessionCookies.add(cookie.replaceAll(";.*", "").replaceAll(c_smsde_uid, tmpSessionCookie + "="));
                        } else if (cookie.toUpperCase().startsWith("C_SMSDE_UID1")) {
                            sessionCookies.add(cookie.replaceAll(";.*", ""));
                        }
                    }
                }
            }
            if (sessionCookies.size() != 2) {
                return Result.LOGIN_FAILED_ERROR();
            }
            return Result.NO_ERROR();
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();
        }
    }

    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        return Result.NO_ERROR();
    }
}
