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

package de.christl.smsoip.supplier.smsglobal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

/**
 *
 */
public class SMSGlobalSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    public static final String LOGIN_BALANCE_URL = "http://www.smsglobal.com/credit-api.php?user=%s&password=%s&country=%s";
    private static final String ENCODING = "UTF-8";


    private final SMSGlobalOptionProvider provider;

    public SMSGlobalSupplier() {
        provider = new SMSGlobalOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        String tmpUrl = String.format(LOGIN_BALANCE_URL, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING), "DE");
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.create().getInputStream();
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        if (!response.contains("CREDITS:")) {
            return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.login_failed));
        }

        return SMSActionResult.LOGIN_SUCCESSFUL();
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInformations(false);
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInformations(false);
    }

    private SMSActionResult refreshInformations(boolean forceUseValidLocale) throws IOException {
        String country = forceUseValidLocale ? "DE" : Locale.getDefault().getCountry();
        String tmpUrl = String.format(LOGIN_BALANCE_URL, URLEncoder.encode(provider.getUserName(), ENCODING), URLEncoder.encode(provider.getPassword(), ENCODING), country);
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.create().getInputStream();
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        return parseBalanceResponse(response);
    }

    private SMSActionResult parseBalanceResponse(String response) throws IOException {
//        CREDITS:29.41;COUNTRY:DE;SMS:10.14;
        if (response.contains("Error 12:")) { //handle invalid country code
            return refreshInformations(true);
        } else if (!response.contains("CREDITS:")) {
            return parseErrorResponse(response);
        }

        String balanceText = provider.getTextByResourceId(R.string.balance);

        String country = response.replaceAll(".*COUNTRY:", "").replaceAll(";.*", "");
        String credits = response.replaceAll(".*CREDITS:", "").replaceAll(";.*", "");
        String smsCredits = response.replaceAll(".*SMS:", "").replaceAll(";.*", "");

        balanceText = String.format(balanceText, credits, country, smsCredits);
        return SMSActionResult.NO_ERROR(balanceText);
    }

    private SMSActionResult parseErrorResponse(String response) {
        return SMSActionResult.UNKNOWN_ERROR("NYI");
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public int getMinuteStepSize() {
        return 1;
    }

    @Override
    public int getDaysInFuture() {
        return 365;
    }

    @Override
    public boolean isSendTypeTimeShiftCapable(String spinnerText) {
        return true;
    }
}
