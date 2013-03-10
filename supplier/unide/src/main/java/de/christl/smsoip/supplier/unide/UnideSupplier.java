/*
 * Copyright (c) Danny Christl 2013.
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

package de.christl.smsoip.supplier.unide;

import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnideSupplier implements ExtendedSMSSupplier {
    private String sessionCookie;
    private OptionProvider provider;
    private static final String ENCODING = "UTF-8";
    private static final String LOGIN_URL = "http://uni.de/login";


    private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String LOGIN_BODY_USER = "user%5Blogin%5D=";
    private static final String LOGIN_BODY_PASS = "&user%5Bpassword%5D=";
    private static final String LOGIN_BODY_SUBMIT = "&submitLogin=";
    private static final String LOGIN_COOKIE_PATTERN = "symfony";
    private static final String LOGIN_SEND_URL = "http://uni.de/sms/send";
    private static final String CHECK_VALID_COOKIE_URL = "http://uni.de/users/edit";
    private static final String SEND_BODY_TEXT = "sendsms%5Btext%5D=";
    private static final String SEND_BODY_NUMBER_PREFIX = "&sendsms%5Bprefix%5D=";
    private static final String SEND_BODY_NUMBER = "&sendsms%5Bnumber%5D=";
    private static final String SEND_BODY_SEND = "&sendsms%5Bonoff%5D=0&send_sms=";

    public UnideSupplier() {
        provider = new UnideOptionProvider();
    }

    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        List<String> sessionCookies;
        if (userName == null || password == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        factory.setFollowRedirects(false);
        factory.setRequestProperties(new HashMap<String, String>() {
            {
                put("Content-Type", APPLICATION_X_WWW_FORM_URLENCODED);
            }
        });


        factory.writeBody(LOGIN_BODY_USER + URLEncoder.encode(userName, ENCODING) + LOGIN_BODY_PASS + URLEncoder.encode(password, ENCODING) + LOGIN_BODY_SUBMIT);


        HttpURLConnection connnection = factory.getConnnection();
        Map<String, List<String>> headerFields = connnection.getHeaderFields();
//        String s = UrlConnectionFactory.inputStream2DebugString(connnection.getInputStream());
        if (headerFields == null || headerFields.size() == 0) {
            return SMSActionResult.NETWORK_ERROR();
        }
        sessionCookies = UrlConnectionFactory.findCookiesByPattern(headerFields, LOGIN_COOKIE_PATTERN + "=.*");
        if (sessionCookies != null && sessionCookies.size() == 2) {
            //take the second cookie, cause it should be newer
            sessionCookie = sessionCookies.get(1);
            factory = new UrlConnectionFactory(CHECK_VALID_COOKIE_URL, UrlConnectionFactory.METHOD_GET);
            factory.setCookies(new ArrayList<String>() {
                {
                    add(sessionCookie);
                }
            });
            HttpURLConnection connnection1 = factory.getConnnection();
            InputStream inputStream = connnection1.getInputStream();
            if (inputStream != null) {
                Document parse = Jsoup.parse(inputStream, ENCODING, "");
                //check if the login panel is available, so its the wrong cookie
                Elements loginPanel = parse.select("div.user_loginPanel");
                if (loginPanel.size() > 0) {
                    sessionCookie = sessionCookies.get(0);
                }
            } else {
                return SMSActionResult.NETWORK_ERROR();
            }

            return SMSActionResult.LOGIN_SUCCESSFUL();
        }

        return SMSActionResult.LOGIN_FAILED_ERROR();
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInfoText(true);
    }

    private SMSActionResult refreshInfoText(boolean loginBefore) throws IOException {
        if (loginBefore) {
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_SEND_URL, UrlConnectionFactory.METHOD_GET);
        factory.setCookies(new ArrayList<String>() {
            {
                add(sessionCookie);
            }
        });

        InputStream inputStream;
        try {
            inputStream = factory.getConnnection().getInputStream();
        } catch (FileNotFoundException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.mobile_not_certified));
            smsActionResult.setRetryMakesSense(false);
            return smsActionResult;
        }
        return parseInfoResponse(inputStream);


    }

    private SMSActionResult parseInfoResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        Document parse = Jsoup.parse(inputStream, ENCODING, "");

        Elements select = parse.select("#formSendSMS p.comm");
        if (select == null || select.size() == 0) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        return SMSActionResult.NO_ERROR(select.text());

    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInfoText(false);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }

        FireSMSResultList out = new FireSMSResultList(receivers.size());
        for (Receiver receiver : receivers) {
            //check the receiver its not from germany and extract it in two parts
            String receiverNumber = receiver.getReceiverNumber();
            if (!receiverNumber.startsWith("0049")) {
                out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR(getProvider().getTextByResourceId(R.string.foreign_numbers_not_allowed))));
                continue;
            }
            //check for number length
            if (receiverNumber.length() < 9) {
                out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR(getProvider().getTextByResourceId(R.string.invalid_number))));
                continue;
            }
            //now extract prefix and number
            String prefix = receiverNumber.substring(4, 7);
            String number = receiverNumber.substring(7, receiverNumber.length());

            UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_SEND_URL, UrlConnectionFactory.METHOD_GET);
            factory.setCookies(new ArrayList<String>() {
                {
                    add(sessionCookie);
                }
            });
            factory.setRequestProperties(new HashMap<String, String>() {
                {
                    put("Content-Type", APPLICATION_X_WWW_FORM_URLENCODED);
                }
            });
            //build and write the body
            String body = SEND_BODY_TEXT + URLEncoder.encode(smsText, ENCODING) + SEND_BODY_NUMBER_PREFIX + prefix + SEND_BODY_NUMBER + number + SEND_BODY_SEND;
            factory.writeBody(body);
            try {
                SMSActionResult smsActionResult = parseResult(factory.getConnnection().getInputStream());
                out.add(new FireSMSResult(receiver, smsActionResult));
            } catch (SocketTimeoutException stoe) {
                Log.e(this.getClass().getCanonicalName(), "SocketTimeoutException", stoe);
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (FileNotFoundException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.mobile_not_certified))));
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "IOException", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }

        }

        return out;
    }

    /**
     * check the return if sending was succesful
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    private SMSActionResult parseResult(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        Elements flashNotice = parse.select("div#flash_notice");
        if (flashNotice.size() != 0) {
            String text = flashNotice.text();
            if (text.contains("erfolgreich")) {
                return SMSActionResult.NO_ERROR(text);
            }
        }
        Elements flashError = parse.select("div#flash_error");
        if (flashError.size() == 0 || flashError.text().equals("")) {
            return SMSActionResult.UNKNOWN_ERROR(getProvider().getTextByResourceId(R.string.not_sent));
        }
        return SMSActionResult.UNKNOWN_ERROR(flashError.text());

    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
