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

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class MeinBMWSupplier implements ExtendedSMSSupplier {

    private final MeinBMWOptionProvider provider;

    private static final String ENCODING = "UTF-8";
    private static final String LOGIN_URL = "https://www.meinbmw.de/Home/tabid/36/ctl/Login/Default.aspx";

    public MeinBMWSupplier() {
        provider = new MeinBMWOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {

        UrlConnectionFactory factory = new UrlConnectionFactory(LOGIN_URL);
        factory.setRequestProperties(new HashMap<String, String>() {{

            put("Host", "www.meinbmw.de");
        }});
        HashMap<String, String> map = new HashMap<String, String>(5);
        map.put("__VIEWSTATE_CACHEKEY", "VS_gjssfcagfnucss55fao1m2es_634952583486610000");
        map.put("__VIEWSTATE", "");
        map.put("dnn$ctr$Login$Login_DNN$txtUsername", userName);
        map.put("dnn$ctr$Login$Login_DNN$txtPassword", password);
        map.put("dnn$ctr$Login$Login_DNN$cmdLogin", "Login");
        factory.writeMultipartBody(map, ENCODING);

        Document parse = Jsoup.parse(factory.getConnnection().getInputStream(), ENCODING, "");
        System.out.println("parse = " + parse);

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
        return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.balance_good));
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInformations(false);
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
