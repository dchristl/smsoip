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

package de.christl.smsoip.supplier.arcor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

/**
 *
 */
public class ArcorSupplier implements ExtendedSMSSupplier {


    private static final Map<String, String> REQUEST_MAP = new HashMap<String, String>() {{
        put("Content-Type", "application/x-www-form-urlencoded");
    }};
    private static final int WITH_PHONE_NUMBER = 1;
    private OptionProvider provider;

    public static final String ENCODING = "ISO-8859-1";

    private static final String HOST = "https://www.arcor.de";
    private static final String LOGIN_URL = HOST + "/login/login.jsp";
    private static final String LOGIN_BODY = "user_name=%s&password=%s&login=Login";
    private static final String BALANCE_SEND_URL = HOST + "/ums/ums_neu_sms.jsp";
    private static final String SEND_BODY = "empfaengerAn=%s&nachricht=%s&senden=Senden";
    private String sessionId;


    public ArcorSupplier() {
        this.provider = new ArcorOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        sessionId = null;
        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        factory.setRequestProperties(REQUEST_MAP);
        String body = String.format(LOGIN_BODY, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING));
        HttpURLConnection httpURLConnection = factory.writeBody(body);
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        if (headerFields == null || headerFields.size() == 0) {
            return SMSActionResult.NETWORK_ERROR();
        }

        String arcorloginstatus = UrlConnectionFactory.findCookieByName(headerFields, "ARCORLOGINSTATUS");
        if (arcorloginstatus == null || !arcorloginstatus.contains("true")) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        sessionId = UrlConnectionFactory.findCookieByName(headerFields, "SessionID");
        if (sessionId == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        return SMSActionResult.LOGIN_SUCCESSFUL();
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
                return result;
            }
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_SEND_URL, UrlConnectionFactory.METHOD_GET);
        factory.setCookies(Collections.<String>singletonList(sessionId));
        InputStream is = factory.getConnnection().getInputStream();

        return parseBalanceResponse(is);
    }

    SMSActionResult parseBalanceResponse(InputStream is) throws IOException {
        if (is == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        Document parse = Jsoup.parse(is, ENCODING, "");

        Elements contentTableContent = parse.select("table.bgGrey3 > tbody > tr >td:eq(1)");
        if (contentTableContent.size() == 0) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        Elements noSMS = contentTableContent.select("td.txtRed");
        if (noSMS.size() == 1) {
            return SMSActionResult.UNKNOWN_ERROR(noSMS.text());
        }
        Elements content = contentTableContent.select("b");
        if (content.size() != 4) {
            return SMSActionResult.UNKNOWN_ERROR(parse.text());
        }


        String balanceText = provider.getTextByResourceId(R.string.balance);


        String freeSMS = content.get(0).text();
        String boughtSMS = content.get(2).text();
        String message = String.format(balanceText, freeSMS, boughtSMS);
        return SMSActionResult.NO_ERROR(message);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        String userName = provider.getUserName();
        SMSActionResult result = checkCredentials(userName, provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_SEND_URL);
        factory.setRequestProperties(REQUEST_MAP);
        factory.setCookies(Collections.<String>singletonList(sessionId));
        StringBuilder receiverString = new StringBuilder();
        for (Receiver receiver : receivers) {
            String receiverNumber = receiver.getReceiverNumber();
            if (!receiverNumber.startsWith("0049")) {
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.sending_to_foreign_not_supported)), receivers);
            }
            receiverString.append(receiverNumber).append("%2C");
        }
        String body = String.format(SEND_BODY, receiverString, URLEncoder.encode(smsText, ENCODING));
        if (provider.getSettings().getBoolean(ArcorOptionProvider.PROVIDER_SAVE_IN_SENT, false)) {
            body += "&gesendetkopiesms=on";
        }
        if (findSendMethod(spinnerText) == WITH_PHONE_NUMBER) {
            body += "&useOwnMobile=on";
        } else {
            String mail = userName.contains("@") ? userName : userName + "@arcor.de";
            body += "&emailAdressen=" + mail;
        }
        InputStream is = factory.writeBody(body).getInputStream();
        return FireSMSResultList.getAllInOneResult(parseSendResponse(is), receivers);
    }

    SMSActionResult parseSendResponse(InputStream is) throws IOException {
        if (is == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        Document parse = Jsoup.parse(is, ENCODING, "");
        Elements select = parse.select("div.error");
        if (select.size() != 0) {
            return SMSActionResult.UNKNOWN_ERROR(select.text());
        }
        select = parse.select(("div.hint"));
        if (select.size() == 0) {

            return SMSActionResult.UNKNOWN_ERROR(select.text());
        }
        return SMSActionResult.NO_ERROR(select.text());
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                return i;
            }
        }
        return WITH_PHONE_NUMBER;
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
