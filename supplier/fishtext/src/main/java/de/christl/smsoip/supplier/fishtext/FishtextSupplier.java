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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;


public class FishtextSupplier implements ExtendedSMSSupplier {


    public static final String SESSION_ID_COOKIE = "SESSIONID";
    private FishtextOptionProvider provider;
    private static final String ENCODING = "UTF-8";

    private static final String BASE_URL = "http://www.fishtext.mobi";
    private static final String LOGIN_URL = BASE_URL + "/cgi-bin/mobi/account";
    private static final String LOGIN_BODY = "action=login&_sp_errorJS=1&_sp_tooltip_init=1&mobile=%s&password=%s";
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
        sessionID = UrlConnectionFactory.findCookieByName(headerFields, SESSION_ID_COOKIE);
        if (sessionID == null || sessionID.equals("")) {
            SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_login_failed));
            smsActionResult.setRetryMakesSense(false);
            return smsActionResult;
        }
        return SMSActionResult.LOGIN_SUCCESSFUL();
    }


    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
