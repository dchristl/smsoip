/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.supplier.okde;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

/**
 *
 */
public class OkDeSMSSupplier implements ExtendedSMSSupplier {


    private static final String ENCODING = "UTF-8";

    private OkDeOptionProvider provider;

    public static final String HOST = "https://appapi.ok.de";
    public static final String LOGIN_URL = HOST + "/1.0/login/?app=adr&version=1";
    public static final String LOGIN_BODY = "user=%s&pass=%s";

    public static final String BALANCE_URL = HOST + "/1.0/sms/getInfo/";
    public static final String BALANCE_BODY = "sessionId=%s";

    public static final String SEND_URL = HOST + "/1.0/sms/send";
    public static final String SEND_BODY = "sessionId=%s&to=%s&txt=%s";

    private String sessionCookie;
    private static final HashMap<String, String> requestMap = new HashMap<String, String>() {
        {
            put("Content-Type", "application/x-www-form-urlencoded");
        }
    };

    public OkDeSMSSupplier() {
        provider = new OkDeOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        String loginBody = String.format(LOGIN_BODY, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING));
        sessionCookie = null;
        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        factory.setRequestProperties(requestMap);
        HttpURLConnection httpURLConnection = factory.writeBody(loginBody);
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        if (headerFields == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        InputStream responseStream = httpURLConnection.getInputStream();
        String returnVal = UrlConnectionFactory.inputStream2DebugString(responseStream);
        if (returnVal != null && returnVal.contains("\"error\":0")) {
            String tmpCookie = UrlConnectionFactory.findCookieByName(headerFields, "PHPSESSID");
            sessionCookie = tmpCookie.replaceAll("PHPSESSID=", "").replaceAll(";.*", "");
        } else if (returnVal != null) {
            return parseErrorResponse(returnVal);
        }
        if (sessionCookie == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        return SMSActionResult.LOGIN_SUCCESSFUL();
    }

    private SMSActionResult parseErrorResponse(String returnVal) {
        String errorText = returnVal.replaceAll(".*\"errorTxt\":\"", "").replaceAll("\".*", "");
        SMSActionResult out = SMSActionResult.UNKNOWN_ERROR();
        if (errorText.length() > 0) {
            if (errorText.equalsIgnoreCase("access denied")) {
                out = SMSActionResult.LOGIN_FAILED_ERROR();
            } else if (errorText.equalsIgnoreCase("session unknown")) {
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.try_again));
            } else if (errorText.equalsIgnoreCase("wrong country")) {
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_foreign));
            } else if (errorText.equalsIgnoreCase("limit reached")) {
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.limit_reached));
            } else {
                out = SMSActionResult.UNKNOWN_ERROR(errorText);
            }
        }
        return out;
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInformations(false);
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInformations(true);
    }

    private SMSActionResult refreshInformations(boolean noLoginBefore) throws IOException {
        if (!noLoginBefore) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                result.setRetryMakesSense(false);
                return result;
            }
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_URL);
        factory.setRequestProperties(requestMap);
        String balanceBody = String.format(BALANCE_BODY, sessionCookie);
        HttpURLConnection httpURLConnection = factory.writeBody(balanceBody);
        InputStream responseStream = httpURLConnection.getInputStream();
        if (responseStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String returnVal = UrlConnectionFactory.inputStream2DebugString(responseStream);
        if (returnVal.contains("\"error\":0")) {
            return parseBalanceResponse(returnVal);
        } else {
            return parseErrorResponse(returnVal);
        }
    }

    private SMSActionResult parseBalanceResponse(String returnVal) {
        String balanceText = provider.getTextByResourceId(R.string.balance);
        String leftSMS = returnVal.replaceAll(".*\"smsLeft\":", "").replaceAll(",\".*", "");
        balanceText = String.format(balanceText, leftSMS);
        return SMSActionResult.NO_ERROR(balanceText);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }

        FireSMSResultList out = new FireSMSResultList();
        for (Receiver receiver : receivers) {
            String receiverNumber = receiver.getReceiverNumber();
            if (!receiverNumber.startsWith("0049")) {
                out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_foreign))));
                continue;
            }

            UrlConnectionFactory factory = new UrlConnectionFactory(SEND_URL);
            factory.setRequestProperties(requestMap);

            String sendBody = String.format(SEND_BODY, sessionCookie, receiverNumber, URLEncoder.encode(smsText, ENCODING));
            try {
                HttpURLConnection httpURLConnection = factory.writeBody(sendBody);
                InputStream responseStream = httpURLConnection.getInputStream();
                if (responseStream == null) {
                    out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
                } else {
                    String returnVal = UrlConnectionFactory.inputStream2DebugString(responseStream);
                    out.add(new FireSMSResult(receiver, parseSendResponse(returnVal)));
                }
            } catch (SocketTimeoutException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }

        }
        return out;
    }

    private SMSActionResult parseSendResponse(String returnVal) {
        SMSActionResult out;
        if (returnVal.contains("\"error\":0")) {
            out = SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.sentSuccess));
        } else {
            out = parseErrorResponse(returnVal);
        }
        return out;
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
