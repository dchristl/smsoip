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

package de.christl.smsoip.supplier.goodmails;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
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
public class GoodmailsSupplier implements ExtendedSMSSupplier {


    private static final String LOGIN_URL = "http://www.goodmails.de/index.php?action=login";
    public static final String HOME_PAGE = "http://goodmails.de/sms.php?action=compose&type=2";
    private static final String TARGET_URL = "http://www.goodmails.de/sms.php?action=sendSMS";
    private static final String PRE_SEND = "http://www.goodmails.de/sms.php?action=compose&type=";
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/34.0.1847.116 Chrome/34.0.1847.116 Safari/537.36";

    private String sidURLString;
    private static final String ENCODING = "UTF-8";

    private OptionProvider provider;

    static final String NOT_ALLOWED_YET = "NOT ALLOWED YET"; ///special case on resend
    static final String MESSAGE_SENT_SUCCESSFUL = "Die SMS wurde erfolgreich verschickt.";


    public GoodmailsSupplier() {
        provider = new GoodmailsOptionProvider();
    }


    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        int sendIndex = findSendMethod(spinnerText);

        Map<String, String> requestProperties = new HashMap<String, String>(1);
        requestProperties.put("Content-Type", "application/x-www-form-urlencoded");
        requestProperties.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        requestProperties.put("Host", "goodmails.de");


        FireSMSResultList out = new FireSMSResultList();

        for (Receiver receiver : receivers) {
            UrlConnectionFactory factory = new UrlConnectionFactory(TARGET_URL + "&" + sidURLString);
            factory.setTargetAgent(USER_AGENT);
//            String token = getToken(sendIndex);
            factory.setRequestProperties(requestProperties);
            try {
                String headerFields = "to=" + receiver.getReceiverNumber() + "&smsText=" + URLEncoder.encode(smsText, ENCODING);
                switch (sendIndex) {
                    case 0: //free
                        headerFields += "&type=freesms";
                        break;
                    case 1:  //standard
                        headerFields += "&type=standardsms";
                        break;
                    default:  //fake
                        headerFields += "&type=aksms";
                        break;
                }

                HttpURLConnection httpURLConnection = factory.writeBody(headerFields);

                Map<String, List<String>> urlConnHeaderFields = httpURLConnection.getHeaderFields();
                if (urlConnHeaderFields == null) {   //normally not reachable cause will be an IOException in getInputStream
                    out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
                    continue;
                }
                out.add(new FireSMSResult(receiver, processResult(httpURLConnection.getInputStream())));

            } catch (SocketTimeoutException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));

            } catch (IOException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }
        }

        return out;
    }

    private String getToken(int sendIndex) throws IOException {
        String url = PRE_SEND + (sendIndex + 1) + "&" + sidURLString;
        UrlConnectionFactory factory = new UrlConnectionFactory(url);
        factory.setTargetAgent(USER_AGENT);
        InputStream inputStream = factory.getConnnection().getInputStream();
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        Elements select = parse.select("input[name=token]");
        if (select.size() > 0) {
            String value = select.first().attr("value");
            if (!value.equals("")) {
                return value;

            }
        }
        return null;

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

        String tmpUrl = HOME_PAGE + "&" + sidURLString;
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        factory.setTargetAgent(USER_AGENT);
        HttpURLConnection urlConnection = factory.create();
        Document parse = Jsoup.parse(urlConnection.getInputStream(), ENCODING, "");
        Elements select = parse.select("div#remainingbalance");
        if (select.size() != 1) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        return SMSActionResult.NO_ERROR("Credits: " + select.text());
    }

    private SMSActionResult processResult(InputStream s) throws IOException {
        if (s == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        Document parse = Jsoup.parse(s, ENCODING, "");
        if (parse.text().equals(NOT_ALLOWED_YET)) {
            return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.alternate_not_allowed_yet));
        }
        Elements select = parse.select("div#sms-message-container");
        if (select.size() == 1 && select.text().equals(MESSAGE_SENT_SUCCESSFUL)) {
            return SMSActionResult.NO_ERROR(MESSAGE_SENT_SUCCESSFUL);
        }
        return SMSActionResult.UNKNOWN_ERROR();
    }


    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException {
        String tmpUrl;
        tmpUrl = LOGIN_URL + "&glf_username=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&glf_password=" +
                URLEncoder.encode(password == null ? "" : password, ENCODING) + "&email_domain=goodmails.de&language=deutsch&do=login";

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        factory.setTargetAgent(USER_AGENT);
        HttpURLConnection httpURLConnection = factory.create();
        InputStream inputStream = httpURLConnection.getInputStream();//fire
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }

        URL url = httpURLConnection.getURL();
        if (url != null && url.getQuery() != null && url.getQuery().contains("sid")) {
            sidURLString = url.getQuery().replaceAll(".*sid", "sid").replaceAll("&.*", "");
            return SMSActionResult.LOGIN_SUCCESSFUL();
        }

        return SMSActionResult.LOGIN_FAILED_ERROR();
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                return i;
            }
        }
        return 0;
    }
}
