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

package de.christl.smsoip.supplier.smsde;


import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.patcher.InputPatcher;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SMSDeSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private SMSDeOptionProvider provider;

    private static final String LOGIN_FIRST_STEP_URL = "http://www.sms.de";
    private static final String LOGIN_SECOND_STEP_URL = "http://www.sms.de/login/refused.php";
    private static final String HOME_PAGE = "http://www.sms.de/index.php";
    private static final String SEND_FREE_PAGE = "http://www.sms.de/sms/sms_send.php";
    private static final String SEND_POWER_PAGE = "http://www.sms.de/sms/sms_send_power.php";
    private Vector<String> sessionCookies;
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
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException {
        return refreshInformations(true);
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        return refreshInformations(false);
    }


    private SMSActionResult refreshInformations(boolean afterMessageSentSuccessful) throws IOException {
        if (!afterMessageSentSuccessful) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        //first get the login cookie
        UrlConnectionFactory factory = new UrlConnectionFactory(HOME_PAGE, UrlConnectionFactory.METHOD_GET);
        factory.setCookies(sessionCookies);
        HttpURLConnection con = factory.create();
        return processRefreshInformations(con.getInputStream());
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
    public OptionProvider getProvider() {
        return provider;
    }


    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException {
        //FIRST STEP
        Vector<String> tmpSessionCookies = new Vector<String>();
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
        tmpSessionCookies.add(smsDeCookie.replaceAll(";.*", ""));
        //now we have the login idependent id cookie
        factory = new UrlConnectionFactory(LOGIN_SECOND_STEP_URL);
        factory.setCookies(tmpSessionCookies);
        factory.setFollowRedirects(false);
        String userNamePasswordBody = "username=" + userName + "&passwd=" + password;
        con = factory.writeBody(userNamePasswordBody);
        headerFields = con.getHeaderFields();
        if (headerFields == null || tmpSessionCookies.size() == 0) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        //get the login cookie
        String tmpSessionCookie = tmpSessionCookies.get(0);
        tmpSessionCookie = tmpSessionCookie.replaceAll("=.*", "");
        sessionCookies = new Vector<String>();
        String cSmsdeUid = "C_SMSDE_UID";
        String c_smsde_uid_cookie = UrlConnectionFactory.findCookieByName(headerFields, cSmsdeUid);
        String c_smsde_uid1_cookie = UrlConnectionFactory.findCookieByName(headerFields, "C_SMSDE_UID1");
        if (c_smsde_uid_cookie == null || c_smsde_uid1_cookie == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        sessionCookies.add(c_smsde_uid_cookie.replaceAll(";.*", "").replaceAll(cSmsdeUid, tmpSessionCookie));
        sessionCookies.add(c_smsde_uid1_cookie.replaceAll(";.*", ""));
        if (sessionCookies.size() != 2) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        return SMSActionResult.LOGIN_SUCCESSFUL();
    }


    private String preCheckNumbers(List<Receiver> receivers) {
        StringBuilder out = new StringBuilder("");
        for (Receiver receiver : receivers) {
            String receiverNumber = receiver.getReceiverNumber();
            if (receiverNumber.length() <= 7) {
                out.append(getProvider().getTextByResourceId(R.string.wrong_number)).append(": ").append(receiver.getReceiverNumber()).append("\n");
            }
        }
        return out.toString();
    }

    private int findSendMethod(String spinnerText, String smsText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = TYPE_FREE;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i;
            }
        }
        if (smsText.length() <= 160) {
            // if 300 is chosen and message is shorter as needed, choose the cheaper send type
            switch (sendType) {
                case TYPE_POWER_300:
                    sendType = TYPE_POWER_160;
                    break;
                case TYPE_POWER_300_SI:
                    sendType = TYPE_POWER_160_SI;
                    break;
                default:
                    break;
            }
        }
        return sendType;
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
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        return sendSMS(smsText, receivers, spinnerText, null);
    }


    private FireSMSResultList sendSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTimeObject) throws IOException {
        String errorText = preCheckNumbers(receivers);
        if (!errorText.equals("")) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(errorText), receivers);
        }
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        int sendIndex = findSendMethod(spinnerText, smsText);
        FireSMSResultList out = new FireSMSResultList();
        for (Receiver receiver : receivers) {
            String receiverNumber = receiver.getReceiverNumber();

            String prefix = receiverNumber.substring(0, 7);
            String number = receiverNumber.substring(7);
            try {
                UrlConnectionFactory factory;
                if (sendIndex == TYPE_FREE) {
                    smsText += " / sms.de";
                }
                StringBuilder body = new StringBuilder(String.format("prefix=%s&target_phone=%s&msg=%s", URLEncoder.encode(prefix, ENCODING), number, URLEncoder.encode(smsText, ENCODING)));
                boolean flagSet = provider.getSettings().getBoolean(InputPatcher.SHOW_RETURN_FROM_SERVER, false);
                switch (sendIndex) {
                    case TYPE_POWER_160:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body.append("&empfcount=1");
                        body.append("&oadc=0");
                        body.append("&smslength=160");
                        body.append("&which_page=power-sms+160");    //this is for output of send type
                        if (!flagSet) {
                            body.append("&which_page_international=power-sms-international+160"); //this is for the return message
                        }
                        break;
                    case TYPE_POWER_160_SI:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body.append("&empfcount=1");
                        body.append("&oadc=1");
                        body.append("&smslength=160");
                        body.append("&which_page=power-sms+160");
                        if (!flagSet) {
                            body.append("&which_page_international=power-sms-international+160");
                        }
                        break;
                    case TYPE_POWER_300:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body.append("&empfcount=1");
                        body.append("&oadc=0");
                        body.append("&smslength=300");
                        body.append("&which_page=power-sms+300");
                        if (!flagSet) {
                            body.append("&which_page_international=power-sms-international+300");
                        }
                        break;
                    case TYPE_POWER_300_SI:
                        factory = new UrlConnectionFactory(SEND_POWER_PAGE);
                        body.append("&empfcount=1");
                        body.append("&oadc=1");
                        body.append("&smslength=300");
                        body.append("&which_page=power-sms+300");
                        if (!flagSet) {
                            body.append("&which_page_international=power-sms-international+300");
                        }
                        break;
                    case TYPE_FREE:
                    default:
                        factory = new UrlConnectionFactory(SEND_FREE_PAGE);
                        body.append("&smslength=151");
                        break;
                }
                if (dateTimeObject != null) {
                    body.append("&schedule=set");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    body.append("&day=").append(sdf.format(dateTimeObject.getCalendar().getTime()));
                    body.append("&hour=").append(dateTimeObject.getHour());
                    body.append("&minute=").append(dateTimeObject.getMinute());
                }
                if (provider.isFlash()) {
                    body.append("&flashsms=1");
                }
                factory.setCookies(sessionCookies);
                HttpURLConnection con = factory.writeBody(body.toString());
                SMSDeSendResult sendResult = processSendReturn(con.getInputStream());
                if (sendResult.isSuccess()) {
                    provider.reset();
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

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException {
        return sendSMS(smsText, receivers, spinnerText, dateTime);
    }

    @Override
    public int getMinuteStepSize() {
        return 10;
    }

    @Override
    public int getDaysInFuture() {
        return 5;
    }

    @Override
    public boolean isSendTypeTimeShiftCapable(String spinnerText) {
        int sendType = findSendMethod(spinnerText, "");
        return sendType != TYPE_FREE;
    }


}
