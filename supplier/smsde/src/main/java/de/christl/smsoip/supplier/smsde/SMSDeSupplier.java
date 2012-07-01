package de.christl.smsoip.supplier.smsde;


import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.patcher.InputPatcher;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
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
        try {
            //first get the login cookie
            UrlConnectionFactory factory = new UrlConnectionFactory(HOME_PAGE, UrlConnectionFactory.METHOD_GET);
            factory.setCookies(sessionCookies);
            HttpURLConnection con = factory.create();
            return processRefreshInformations(con.getInputStream());
        } catch (SocketTimeoutException stoe)

        {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return SMSActionResult.NETWORK_ERROR();

        }
    }

    SMSActionResult processRefreshInformations(InputStream inputStream) throws IOException {
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        //search for all link forwarding to account and Credits are inside text
        Elements farbLinks = parse.select("a.farb[href~=konto]:matches(Credits)");
        String credits = farbLinks.text();
        credits = credits.replaceAll("[^\\p{Print}]", "").trim(); //remove all non printable lines
        if (!credits.equals("")) {
            return SMSActionResult.NO_ERROR(credits);
        } else {
            return SMSActionResult.UNKNOWN_ERROR();
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
    public SMSActionResult checkCredentials(String userName, String password) {
        //FIRST STEP
        sessionCookies = new ArrayList<String>();
        try {
            UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_FIRST_STEP_URL, UrlConnectionFactory.METHOD_GET);
            //first get the login cookie
            HttpURLConnection con = factory.create();
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null) {
                return SMSActionResult.NETWORK_ERROR();
            }
            String firstCookie = "C_SMSDE_ID";
            String firstCookiePattern = firstCookie + ".*=.*";
            String smsDeCookie = UrlConnectionFactory.findCookieByPattern(headerFields, firstCookiePattern);
            if (smsDeCookie == null) {
                return SMSActionResult.NETWORK_ERROR(); //not possible if network available
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
                return SMSActionResult.LOGIN_FAILED_ERROR();
            }
            //get the login cookie
            String tmpSessionCookie = sessionCookies.get(0);
            tmpSessionCookie = tmpSessionCookie.replaceAll("=.*", "");
            sessionCookies = new ArrayList<String>();
            String cSmsdeUid = "C_SMSDE_UID";
            String c_smsde_uid_cookie = UrlConnectionFactory.findCookieByName(headerFields, cSmsdeUid);
            if (c_smsde_uid_cookie == null) {
                return SMSActionResult.LOGIN_FAILED_ERROR();
            }
            sessionCookies.add(c_smsde_uid_cookie.replaceAll(";.*", "").replaceAll(cSmsdeUid, tmpSessionCookie));
            String c_smsde_uid1_cookie = UrlConnectionFactory.findCookieByName(headerFields, "C_SMSDE_UID1");
            sessionCookies.add(c_smsde_uid1_cookie.replaceAll(";.*", ""));
            if (sessionCookies.size() != 2) {
                return SMSActionResult.LOGIN_FAILED_ERROR();
            }
            return SMSActionResult.LOGIN_SUCCESSFUL();
        } catch (SocketTimeoutException stoe) {
            Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "IOException", e);
            return SMSActionResult.NETWORK_ERROR();
        }
    }


    private String preCheckNumbers(List<Receiver> receivers) {
        StringBuilder out = new StringBuilder("");
        for (Receiver receiver : receivers) {
            String receiverNumber = receiver.getReceiverNumber();
            if (receiverNumber.length() <= 7) {
                out.append(getProvider().getTextByResourceId(R.string.text_wrong_number)).append(": ").append(receiver.getReceiverNumber()).append("\n");
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

    SMSDeSendResult processSendReturn(InputStream is) throws IOException {
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
                Elements a = nextTD.select("a");
                for (Element element : a) {
                    element.remove();
                }
                returnMessage = nextTD.text();
                returnMessage = returnMessage.replaceAll(nextTD.select("p").text(), "");
                returnMessage = returnMessage.replaceAll("[^\\p{Print}]", "").trim();
                returnMessage = returnMessage.replaceAll("  .*", ""); //replace extra signs at the end
                break;
            }
        }
        if (returnMessage != null) {
            returnMessage = returnMessage.replaceAll(":", ""); //cut the colons at the end of the line
        }
        return new SMSDeSendResult(success, returnMessage);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) {
        String errorText = preCheckNumbers(receivers);
        if (!errorText.equals("")) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(errorText));
        }
        int sendIndex = findSendMethod(spinnerText);
        FireSMSResultList out = new FireSMSResultList();
        for (Receiver receiver : receivers) {
            String receiverNumber = receiver.getReceiverNumber();
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                out.add(new FireSMSResult(receiver, result));
                continue;
            }
            String prefix = receiverNumber.substring(0, 7);
            String number = receiverNumber.substring(7);
            try {
                UrlConnectionFactory factory;
                String body = String.format("prefix=%s&target_phone=%s&msg=%s", URLEncoder.encode(prefix, ENCODING), number, URLEncoder.encode(smsText, ENCODING));
                boolean paidDisabled = provider.getSettings().getBoolean(InputPatcher.DISABLE_PAID_SMS_IN_SMS_DE, false);
                switch (sendIndex) {
                    case TYPE_POWER_160:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=0";
                        body += "&smslength=160";
                        body += "&which_page=power-sms+160";    //this is for output of send type
                        if (!paidDisabled) {
                            body += "&which_page_international=power-sms-international+160"; //this one makes paid sms OMG
                        }
                        break;
                    case TYPE_POWER_160_SI:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=1";
                        body += "&smslength=160";
                        body += "&which_page=power-sms+160";
                        if (!paidDisabled) {
                            body += "&which_page_international=power-sms-international+160";
                        }
                        break;
                    case TYPE_POWER_300:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=0";
                        body += "&smslength=300";
                        body += "&which_page=power-sms+300";
                        if (!paidDisabled) {
                            body += "&which_page_international=power-sms-international+300";
                        }
                        break;
                    case TYPE_POWER_300_SI:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body += "&empfcount=1";
                        body += "&oadc=1";
                        body += "&smslength=300";
                        body += "&which_page=power-sms+300";
                        if (!paidDisabled) {
                            body += "&which_page_international=power-sms-international+300";
                        }
                        break;
                    case TYPE_FREE:
                    default:
                        factory = new UrlConnectionFactory(SEND_FREE_PAGE);
                        body += "&smslength=151";
                        break;
                }
                factory.setCookies(sessionCookies);
                HttpURLConnection con = factory.writeBody(body);
                SMSDeSendResult sendResult = processSendReturn(con.getInputStream());
                if (sendResult.isSuccess()) {
                    out.add(new FireSMSResult(receiver, SMSActionResult.NO_ERROR(sendResult.getMessage())));
                } else {
                    out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR(sendResult.getMessage())));
                }
            } catch (SocketTimeoutException stoe) {
                Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));

            }
        }

        return out;
    }
}
