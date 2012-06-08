package de.christl.smsoip.supplier.smsde;


import android.text.Editable;
import android.util.Log;
import de.christl.smsoip.connection.UrlConnectionFactory;
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

            return processSendReturn(con.getInputStream());
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();

        }
    }

    private Result processSendReturn(InputStream is) throws IOException {
        String s = UrlConnectionFactory.inputStream2DebugString(is, ENCODING);
        System.out.println(s);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, ENCODING));
        String line;
        String returnMessage = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains("fbrb") && line.contains("/images/")) {
                returnMessage = line.replaceAll(".*\">", "");
                returnMessage = returnMessage.replaceAll("<.*", "");
                returnMessage = returnMessage.replaceAll("[[^0-9]&&[^A-z]&&[^ ]].*", "");
                break;
            }
        }
        if (returnMessage != null) {
            return Result.NO_ERROR().setAlternateText(returnMessage);
        } else {
            return Result.UNKNOWN_ERROR();
        }
    }

}
