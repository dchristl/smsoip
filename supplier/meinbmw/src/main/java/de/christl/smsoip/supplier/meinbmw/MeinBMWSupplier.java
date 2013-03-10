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

package de.christl.smsoip.supplier.meinbmw;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeinBMWSupplier implements ExtendedSMSSupplier {

    public static final String VIEWSTATE_CACHEKEY = "__VIEWSTATE_CACHEKEY";
    public static final String VIEWSTATE = "__VIEWSTATE";
    public static final String VS_GJSSFCAGFNUCSS_55_FAO_1_M_2_ES_634952583486610000CACHE_KEY_CONTENT = "VS_gjssfcagfnucss55fao1m2es_634952583486610000";
    private final MeinBMWOptionProvider provider;

    private static final String ENCODING = "UTF-8";
    private static final String LOGIN_COOKIE_PATTERN = "\\.DOTNETNUKE=.*";
    private static final String LOGIN_URL = "https://www.meinbmw.de/Home/tabid/36/ctl/Login/Default.aspx";
    private static final String TARGET_URL = "https://www.meinbmw.de/tabid/80/Default.aspx";
    private List<String> sessionCookies;

    public MeinBMWSupplier() {
        provider = new MeinBMWOptionProvider();
    }

    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {

        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        sessionCookies = null;
        factory.setFollowRedirects(false);
        HashMap<String, String> map = new HashMap<String, String>(5);
        map.put(VIEWSTATE_CACHEKEY, VS_GJSSFCAGFNUCSS_55_FAO_1_M_2_ES_634952583486610000CACHE_KEY_CONTENT);
        map.put(VIEWSTATE, "");
        map.put("dnn$ctr$Login$Login_DNN$txtUsername", userName);
        map.put("dnn$ctr$Login$Login_DNN$txtPassword", password);
        map.put("dnn$ctr$Login$Login_DNN$cmdLogin", "Login");
        factory.writeMultipartBody(map, ENCODING);
        Map<String, List<String>> headerFields = factory.getConnnection().getHeaderFields();
        if (headerFields == null || headerFields.size() == 0) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String cookiesByPattern = UrlConnectionFactory.findCookieByPattern(headerFields, LOGIN_COOKIE_PATTERN);
        if (cookiesByPattern != null && cookiesByPattern.length() > 1) {
            sessionCookies = UrlConnectionFactory.findCookiesByPattern(headerFields, ".*");
            return SMSActionResult.LOGIN_SUCCESSFUL();
        }

        return SMSActionResult.LOGIN_FAILED_ERROR();
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInformations(true);
    }

    private SMSActionResult refreshInformations(boolean loginBefore) throws IOException {
        if (loginBefore) {
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(TARGET_URL, UrlConnectionFactory.METHOD_GET);
        factory.setCookies(sessionCookies);
        Document parse = Jsoup.parse(factory.getConnnection().getInputStream(), ENCODING, "");

        Elements select = parse.select("textarea.smstextarea"); //check if the textarea field is visible to check if free messages are available
        if (select.size() == 1) {
            return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.balance_good));
        }
        return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.balance_bad));
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInformations(false);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }

        FireSMSResultList out = new FireSMSResultList(receivers.size());
        for (Receiver receiver : receivers) {
            UrlConnectionFactory factory = new UrlConnectionFactory(TARGET_URL);
            factory.setCookies(sessionCookies);
            HashMap<String, String> map = new HashMap<String, String>(5);
            map.put(VIEWSTATE_CACHEKEY, VS_GJSSFCAGFNUCSS_55_FAO_1_M_2_ES_634952583486610000CACHE_KEY_CONTENT);
            map.put(VIEWSTATE, "");
            map.put("dnn$ctr493$SfiSms_View$phone", receiver.getReceiverNumber());
            map.put("dnn$ctr493$SfiSms_View$subject", smsText);
            map.put("dnn$ctr493$SfiSms_View$sendData", "Senden");
            factory.writeMultipartBody(map, ENCODING);
            try {
                SMSActionResult smsActionResult = parseResult(factory.getConnnection().getInputStream());
                out.add(new FireSMSResult(receiver, smsActionResult));
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

    private SMSActionResult parseResult(InputStream inputStream) throws IOException {
        Document parse = Jsoup.parse(inputStream, ENCODING, "");
        Elements select = parse.select("span#dnn_ctr493_SfiSms_View_lb_error");
        String textByResourceId = provider.getTextByResourceId(R.string.not_sent);
        if (select == null || select.size() == 0) {
            return SMSActionResult.UNKNOWN_ERROR(textByResourceId);
        }
        String text = select.text();
        if (text.equals("")) {
            Elements parents = select.parents();
            if (parents == null || parents.size() == 0) {
                return SMSActionResult.UNKNOWN_ERROR(textByResourceId);
            }

            return SMSActionResult.UNKNOWN_ERROR(parents.first().text());
        }
        return SMSActionResult.NO_ERROR(text);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
