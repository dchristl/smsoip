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

package de.christl.smsoip.supplier.fishtext;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class FishtextSupplier implements ExtendedSMSSupplier {


    public static final String SESSION_ID_COOKIE = "sessionID";
    /**
     * the message box name, seems to be different on accounts, but will not be checked really,
     * if it could not be resolved this is the fallback (id of empty test account)
     */
    public static final String MESSAGE_BOX_NAME = "Md09f227bf5931a1d31893782d395ea15";
    private FishtextOptionProvider provider;
    private static final String ENCODING = "ISO-8859-1";

    private static final String BASE_URL = "http://www.fishtext.mobi";
    private static final String LOGIN_URL = BASE_URL + "/cgi-bin/mobi/account";
    private static final String BALANCE_URL = BASE_URL + "/cgi-bin/mobi/sendMessage.cgi";
    private static final String LOGIN_BODY = "action=login&_sp_errorJS=1&_sp_tooltip_init=1&mobile=%s&password=%s";
    private static final String SEND_MESSAGE_URL = BASE_URL + "/SendSMS/SendSMS";
    private static final String SEND_BODY = "action=Send&SA=%s&DR=1&ST=1&RN=%s&%s=%s";


    private String sessionID;


    public FishtextSupplier() {
        provider = new FishtextOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        HttpURLConnection con = factory.writeBody(String.format(LOGIN_BODY, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING)));
        Map<String, List<String>> headerFields = con.getHeaderFields();
        if (headerFields == null || headerFields.size() == 0) {
            SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_login_failed));
            smsActionResult.setRetryMakesSense(false);
            return smsActionResult;
        }
        sessionID = UrlConnectionFactory.findCookieByName(headerFields, SESSION_ID_COOKIE.toUpperCase());
        if (sessionID == null || sessionID.equals("")) {
            SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_login_failed));
            smsActionResult.setRetryMakesSense(false);
            return smsActionResult;
        }
        return SMSActionResult.LOGIN_SUCCESSFUL();
    }


    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException {
        return refreshInformations(true);
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        return refreshInformations(false);
    }


    private synchronized SMSActionResult refreshInformations(boolean afterMessageSentSuccessful) throws IOException {
        if (!afterMessageSentSuccessful) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_URL);
        factory.setCookies(new ArrayList<String>() {
            {
                add(sessionID);
            }

        });
        InputStream inputStream = factory.create().getInputStream();
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        String text = parse.select("#balanceCounter").text();
        if (text != null && !text.equals("")) {
            text = String.format(provider.getTextByResourceId(R.string.text_balance), text);
            return SMSActionResult.NO_ERROR(text);
        }
        return SMSActionResult.UNKNOWN_ERROR();
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws
            IOException, NumberFormatException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(SEND_MESSAGE_URL);
        factory.setCookies(new ArrayList<String>() {
            {
                add(sessionID);
            }

        });
        StringBuilder receiverListBuilder = new StringBuilder();
        for (int i = 0, receiversSize = receivers.size(); i < receiversSize; i++) {
            String receiver = receivers.get(i).getReceiverNumber();
            receiverListBuilder.append(receiver);
            if (i + 1 != receivers.size()) {
                receiverListBuilder.append(",");
            }
        }
        int sendType = findSendMethod(spinnerText);
        String messageBoxName = findMessageBoxName();
        HttpURLConnection urlConnection = factory.writeBody(String.format(SEND_BODY, sendType, receiverListBuilder.toString(), messageBoxName, URLEncoder.encode(smsText, ENCODING)));
        InputStream inputStream = urlConnection.getInputStream();
        SMSActionResult smsActionResult = processReturnMessage(inputStream);
        return FireSMSResultList.getAllInOneResult(smsActionResult, receivers);

    }

    private String findMessageBoxName() {
        UrlConnectionFactory factory = new UrlConnectionFactory(BALANCE_URL);
        factory.setCookies(new ArrayList<String>() {
            {
                add(sessionID);
            }

        });
        Document parse;
        try {
            InputStream inputStream = factory.create().getInputStream();
            parse = Jsoup.parse(inputStream, ENCODING, "");
            String messageId = parse.select("#message").attr("name");
            if (messageId != null && !messageId.equals("")) {
                return messageId;
            }
        } catch (IOException e) {
            return MESSAGE_BOX_NAME;
        }
        return MESSAGE_BOX_NAME;
    }

    static SMSActionResult processReturnMessage(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        Elements result = parse.select("h1");
        if (result.size() == 0) {
            result = parse.select("h2");
        }
        Elements content = parse.select("p");
        if (result.size() != 1 || content.size() != 1) { //don't know whats getting wrong here
            return SMSActionResult.UNKNOWN_ERROR(parse.text());
        }
        String message = content.text();
        if (!result.text().equals("Message sent")) {   //error occured
            return SMSActionResult.UNKNOWN_ERROR(message);
        }
        return SMSActionResult.NO_ERROR(message);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = 0;//SOURCE_IDENTIFIER;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i;
                break;
            }
        }

        return sendType;
    }

}
