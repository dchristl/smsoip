package de.christl.smsoip.supplier.smsde;


import android.text.Editable;
import android.util.Log;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

public class SMSDeSupplier implements ExtendedSMSSupplier {

    private SMSDeOptionProvider provider;

    private static final String LOGIN_FIRST_STEP_URL = "http://www.sms.de";
    private static final String LOGIN_SECOND_STEP_URL = "http://www.sms.de/login/refused.php";
    private static final String HOME_PAGE = "http://www.sms.de/index.php";
    private static final String SEND_FREE_PAGE = "http://www.sms.de/sms/sms_send.php";
    private static final String SEND_POWER_PAGE = "http://www.sms.de/sms/sms_send_power.php";
    private List<String> sessionCookies;
    private static final String ENCODING = "ISO-8859-1";


    private static final int TYPE_FREE = 0;
    private static final int TYPE_POWER_160 = 1;
    private static final int TYPE_POWER_160_SI = 2;
    private static final int TYPE_POWER_300 = 3;
    private static final int TYPE_POWER_300_SI = 4;


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
        return Result.UNKNOWN_ERROR().setAlternateText("Deprecated API");
    }

    private String buildMessageText(List<SendResult> sendResults) {
        StringBuilder out = new StringBuilder();
        for (SendResult sendResult : sendResults) {
            out.append(sendResult.getNumber()).append("->").append(sendResult.getReturnMessage()).append("\n");
        }
        return out.toString();
    }

    private String preCheckNumber(List<String> receivers) {
        StringBuilder out = new StringBuilder("");
        for (String receiver : receivers) {
            if (receiver.length() <= 7) {
                out.append(getProvider().getTextByResourceId(R.string.text_wrong_number)).append(": ").append(receiver).append("\n");
            }
        }
        return out.toString();
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                return i;
            }
        }
        return TYPE_FREE;
    }

    SendResult processSendReturn(InputStream is, String number) throws IOException {
        Document parse = Jsoup.parse(is, ENCODING, "");
        Elements tDsWithImages = parse.select("td.fbrb > table:eq(0)  tr:eq(0) > td.fbrb:has(img[align=absmiddle])");
        boolean success = false;
        String returnMessage = null;
        for (Element nextTD : tDsWithImages) {
            Elements imageTag = nextTD.select(">img");
            if (imageTag.size() > 0) { //now its the correct tag
                if (imageTag.outerHtml().contains("gruen")) {
                    success = true;
                }
                returnMessage = nextTD.text();
                returnMessage = returnMessage.replaceAll(nextTD.select("p").text(), "");
                returnMessage = returnMessage.replaceAll("[^\\p{Print}]", "").trim();
                break;
            }
        }
        return new SendResult(number, success, returnMessage);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<String> receivers, String spinnerText) {
        String errorText = preCheckNumber(receivers);
        if (!errorText.equals("")) {
//            return Result.UNKNOWN_ERROR().setAlternateText(errorText);
        }
        int sendIndex = findSendMethod(spinnerText);
        boolean succesful = true;
        List<SendResult> sendResults = new ArrayList<SendResult>(receivers.size());
        for (String receiverNumber : receivers) {
            Result result = login(provider.getUserName(), provider.getPassword());
            if (!result.equals(Result.NO_ERROR)) {
//                return result;
            }
            String prefix = receiverNumber.substring(0, 7);
            String number = receiverNumber.substring(7);
            try {
                UrlConnectionFactory factory;
                String body = String.format("prefix=%s&target_phone=%s&msg=%s", URLEncoder.encode(prefix, ENCODING), number, URLEncoder.encode(smsText, ENCODING));
                switch (sendIndex) {
                    case TYPE_POWER_160:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=0";
                        body += "&smslength=160";
                        break;
                    case TYPE_POWER_160_SI:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=1";
                        body += "&smslength=160";
                        break;
                    case TYPE_POWER_300:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=0";
                        body += "&smslength=300";
                        break;
                    case TYPE_POWER_300_SI:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=1";
                        body += "&smslength=300";
                        break;
                    case TYPE_FREE:
                    default:
                        factory = new UrlConnectionFactory(SEND_FREE_PAGE);
                        body += "&smslength=151";
                        break;
                }
                factory.setCookies(sessionCookies);
                HttpURLConnection con = factory.writeBody(body);
                SendResult sendResult = processSendReturn(con.getInputStream(), receiverNumber);
                succesful &= sendResult.isSuccess();
                sendResults.add(sendResult);
            } catch (SocketTimeoutException stoe) {
                Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
//                return Result.TIMEOUT_ERROR().setAlternateText(buildMessageText(sendResults));
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
//                return Result.NETWORK_ERROR().setAlternateText(buildMessageText(sendResults));

            }
        }
        String messageText = buildMessageText(sendResults);
        if (succesful) {
//            return Result.NO_ERROR().setAlternateText(messageText);
        }
        return new FireSMSResultList();//Result.UNKNOWN_ERROR().setAlternateText(messageText);
    }
}
