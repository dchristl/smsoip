package de.christl.smsoip.supplier.freenet;

import android.text.Editable;
import android.text.Html;
import android.util.Log;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for handling sms by freenet
 */
public class FreenetSupplier implements SMSSupplier {

    private FreenetOptionProvider provider;

    private static final String LOGIN_URL = "https://auth.freenet.de/portal/login.php";
    private static final String HOME_URL = "http://webmail.freenet.de/login/index.html";
    private static final String REFRESH_URL = "http://webmail.freenet.de/Global/Action/StatusBarGet";
    private static final String SEND_URL = "http://webmail.freenet.de/Sms/Action/Send?myAction=send&";
    private List<String> sessionCookies;
    private static final String ENCODING = "ISO-8859-1";

    public FreenetSupplier() {
        provider = new FreenetOptionProvider();
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
        String tmpUrl = REFRESH_URL;
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(tmpUrl).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("GET");
            StringBuilder cookieBuilder = new StringBuilder();
            for (String sessionCookie : sessionCookies) {
                cookieBuilder.append(sessionCookie).append(";");
            }
            con.setRequestProperty("Cookie", cookieBuilder.toString());
            return processRefreshReturn(con.getInputStream());
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();
        }
    }

    private Result processRefreshReturn(InputStream is) throws IOException {
        String message = inputStream2String(is);
        String out = provider.getTextByResourceId(R.string.text_refresh_informations);
        Pattern p = Pattern.compile("SMS.*?\\}"); //get the SMS JSON object
        Matcher m = p.matcher(message);
        while (m.find()) {
            String messageJSONObject = message.substring(m.start(), m.end());
            p = Pattern.compile("[0-9]+");
            m = p.matcher(messageJSONObject);
            Integer allSMS = null;
            Integer paidSMS = null;
            while (m.find()) {
                if (allSMS == null) {
                    allSMS = Integer.parseInt(messageJSONObject.substring(m.start(), m.end()));
                } else if (paidSMS == null) {
                    paidSMS = Integer.parseInt(messageJSONObject.substring(m.start(), m.end()));
                } else {
                    break;
                }
            }
            if (allSMS != null && paidSMS != null) {
                return Result.NO_ERROR().setAlternateText(String.format(out, allSMS, paidSMS));
            }

        }
        return Result.UNKNOWN_ERROR();
    }

    private Result processFireSMSReturn(InputStream is) throws IOException {
        String message = inputStream2String(is);
        message = message.replaceAll(".*SMSnotify\" value=\"", "");
        message = message.replaceAll("\">.*", "");
        message = Html.fromHtml(message).toString();
        if (message.contains("erfolgreich")) {
            return Result.NO_ERROR().setAlternateText(message);
        } else {
            return Result.UNKNOWN_ERROR().setAlternateText(message);
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

    @Override
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
                return Result.LOGIN_FAILED_ERROR();
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
                return Result.LOGIN_FAILED_ERROR();
            }
            //now get the freenetMail4Prev cookie, that is also needed
            con = (HttpURLConnection) new URL(HOME_URL).openConnection();
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", TARGET_AGENT);
            con.setRequestMethod("POST");
            con.setRequestProperty("Cookie", sidCookie);
            headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return Result.LOGIN_FAILED_ERROR();
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
        return fireSMS(smsText.toString(), receivers.get(0).toString());
    }

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
        //currently only free sms supported, for paid accounts change will be here
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
            return processFireSMSReturn(con.getInputStream());
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return Result.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return Result.NETWORK_ERROR();
        }
    }
}
