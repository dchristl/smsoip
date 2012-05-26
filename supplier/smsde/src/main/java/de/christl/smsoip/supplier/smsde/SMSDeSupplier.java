package de.christl.smsoip.supplier.smsde;


import android.text.Editable;
import android.util.Log;
import connection.UrlConnectionFactory;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SMSDeSupplier implements SMSSupplier {


    private SMSDeOptionProvider provider;

    private static final String LOGIN_FIRST_STEP_URL = "http://www.sms.de";
    private static final String LOGIN_SECOND_STEP_URL = "http://www.sms.de/login/refused.php";
    private static final String HOME_PAGE = "http://www.sms.de/index.php";
    private static final String SEND_PAGE = "http://www.sms.de/sms/sms_send.php";
    private List<String> sessionCookies;
    private static final String ENCODING = "ISO-8859-1";

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
            if (!result.equals(Result.NO_ERROR)) {
                return result;
            }
        }
        try {
            //first get the login cookie
            UrlConnectionFactory factory = new UrlConnectionFactory(HOME_PAGE, UrlConnectionFactory.METHOD_GET);
            factory.setCookies(sessionCookies);
            HttpURLConnection con = factory.create();
            return processRefreshInformations(con.getInputStream());
        } catch (SocketTimeoutException stoe)

        {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();

        }
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
            UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_FIRST_STEP_URL, UrlConnectionFactory.METHOD_GET);
            //first get the login cookie
            HttpURLConnection con = factory.create();
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.NETWORK_ERROR();
            }
            String FIRST_COOKIE = "C_SMSDE_ID";
            String firstCookiePattern = FIRST_COOKIE + ".*=.*";
            String smsDeCookie = UrlConnectionFactory.findCookieByPattern(headerFields, firstCookiePattern);
            if (smsDeCookie == null) {
                return Result.NETWORK_ERROR(); //not possible if network available
            }
            sessionCookies.add(smsDeCookie.replaceAll(";.*", ""));
            //now we have the login idependent id cookie
            factory = new UrlConnectionFactory(LOGIN_SECOND_STEP_URL);
            factory.setCookies(sessionCookies);
            factory.setFollowRedirects(false);
            String userNamePasswordBody = "username=" + userName + "&passwd=" + password;
            con = factory.writeBody(userNamePasswordBody);
            headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.LOGIN_FAILED_ERROR();
            }
            //get the login cookie
            String tmpSessionCookie = sessionCookies.get(0);
            tmpSessionCookie = tmpSessionCookie.replaceAll("=.*", "");
            sessionCookies = new ArrayList<String>();
            String C_SMSDE_UID = "C_SMSDE_UID";
            String c_smsde_uid_cookie = UrlConnectionFactory.findCookieByName(headerFields, C_SMSDE_UID);
            if (c_smsde_uid_cookie == null) {
                return Result.LOGIN_FAILED_ERROR();
            }
            sessionCookies.add(c_smsde_uid_cookie.replaceAll(";.*", "").replaceAll(C_SMSDE_UID, tmpSessionCookie));
            String c_smsde_uid1_cookie = UrlConnectionFactory.findCookieByName(headerFields, "C_SMSDE_UID1");
            sessionCookies.add(c_smsde_uid1_cookie.replaceAll(";.*", ""));
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

    //    POST /sms/sms_send.php HTTP/1.1
//    Host: sms.de
//    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0
//    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//    Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//    Accept-Encoding: gzip, deflate
//    DNT: 1
//    Proxy-Connection: keep-alive
//    Referer: http://sms.de/sms/sms_free.php
//    Content-Type: application/x-www-form-urlencoded
//    Content-Length: 173
//
    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        Result result = login(provider.getUserName(), provider.getPassword());
        if (!result.equals(Result.NO_ERROR)) {
            return result;
        }
        String onlyOneReceiver = receivers.get(0).toString();
        String prefix;
        String number;
        if (onlyOneReceiver.length() > 7) {
            prefix = onlyOneReceiver.substring(0, 7);
            number = onlyOneReceiver.substring(7);
        } else {
            return Result.UNKNOWN_ERROR().setAlternateText(getProvider().getTextByResourceId(R.string.text_wrong_number));
        }
        try {
            UrlConnectionFactory factory = new UrlConnectionFactory(SEND_PAGE);
            factory.setCookies(sessionCookies);

            String body = String.format("prefix=%s&target_phone=%s&msg=%s&smslength=151", URLEncoder.encode(prefix, ENCODING), number, URLEncoder.encode(smsText.toString(), ENCODING));
            HttpURLConnection con = factory.writeBody(body);

            String s = UrlConnectionFactory.inputStream2DebugString(con.getInputStream());
            System.out.println(s);
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();

        }
        return Result.NO_ERROR();
    }

    public Result fireSMSByText(String ha, ArrayList<String> strings, String asas) {
        try {
            UrlConnectionFactory factory = new UrlConnectionFactory(SEND_PAGE);
            factory.setCookies(sessionCookies);
            String body = String.format("prefix=%s&target_phone=%s&msg=%s&smslength=151", URLEncoder.encode("004917", ENCODING), "42383886", URLEncoder.encode("  Test wwrong number  ", ENCODING));
            HttpURLConnection con = factory.writeBody(body);

            String s = UrlConnectionFactory.inputStream2DebugString(con.getInputStream());
            System.out.println(s);
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();

        }
        return Result.NO_ERROR();
    }
}
