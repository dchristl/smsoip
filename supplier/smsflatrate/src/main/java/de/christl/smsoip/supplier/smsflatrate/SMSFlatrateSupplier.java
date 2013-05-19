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

package de.christl.smsoip.supplier.smsflatrate;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;

public class SMSFlatrateSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {
    private OptionProvider provider;


    private static final String API_BASE = "http://www.smsflatrate.net/appkey.php?appkey=%s&lizenz=217075022&aid=4949&";
    private static final String INFO_TEXT_URL = API_BASE + "request=credits";

    private static final String ENCODING = "UTF-8";

    public SMSFlatrateSupplier() {
        provider = new SMSFlatrateOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        //use the refresh URL for
        SMSActionResult refreshResult = refreshInfoText(password);
        if (refreshResult.isSuccess()) {
            return SMSActionResult.LOGIN_SUCCESSFUL();
        } else {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
    }

    private SMSActionResult refreshInfoText(String password) throws IOException {
        String tmpUrl = String.format(INFO_TEXT_URL, URLEncoder.encode(password, ENCODING));

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        HttpURLConnection connnection = factory.getConnnection();
        InputStream inputStream = connnection.getInputStream();
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        if (connnection.getResponseCode() == HttpURLConnection.HTTP_OK && response != null && response.contains(".")) {
            return SMSActionResult.NO_ERROR(String.format(provider.getTextByResourceId(R.string.balance), Double.parseDouble(response)));
        }
        return SMSActionResult.UNKNOWN_ERROR();
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInfoText(provider.getPassword());
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInfoText(provider.getPassword());
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        return fireTimeShiftSMS(smsText, receivers, spinnerText, null);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException, NumberFormatException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getMinuteStepSize() {
        return 1;
    }

    @Override
    public int getDaysInFuture() {
        return 360;
    }

    @Override
    public boolean isSendTypeTimeShiftCapable(String spinnerText) {
        return true;
    }
}
