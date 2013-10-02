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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final String LOGIN_BALANCE_URL = "http://www.smsglobal.com/http-api.php?user=%s&password=%s&action=balancesms";
    private static final String ENCODING = "UTF-8";


    private final SMSGlobalOptionProvider provider;

    public SMSGlobalSupplier() {
        provider = new SMSGlobalOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        String tmpUrl = String.format(LOGIN_BALANCE_URL, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING));
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.create().getInputStream();
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        if (!response.contains("BALANCE:")) {
            return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.login_failed));
        }

        return SMSActionResult.LOGIN_SUCCESSFUL();
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInformations();
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return refreshInformations();
    }

    private SMSActionResult refreshInformations() throws IOException {
        String tmpUrl = String.format(LOGIN_BALANCE_URL, URLEncoder.encode(provider.getUserName(), ENCODING), URLEncoder.encode(provider.getPassword(), ENCODING));
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.create().getInputStream();
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        return parseBalanceResponse(response);
    }

    private SMSActionResult parseBalanceResponse(String response) throws IOException {
//        BALANCE: 0.65220002021376; USER:
        if (!response.contains("BALANCE:")) {
            return parseErrorResponse(response);
        }

        String balanceText = provider.getTextByResourceId(R.string.balance);

        String balanceS = response.replaceAll(".*BALANCE:", "").replaceAll(";.*", "");
        double balance = Double.parseDouble(balanceS);
        balanceText = String.format(balanceText, balance);
        return SMSActionResult.NO_ERROR(balanceText);
    }

    private SMSActionResult parseErrorResponse(String response) {

//        Error 10: Missing login details
        Pattern p = Pattern.compile("Error ([0-9]+):.*");
        Matcher m = p.matcher(response);
        int errorCode = -1;
        if (m.matches()) {
            String errorCodeText = m.group(1);
            if (errorCodeText != null) {
                try {
                    errorCode = Integer.parseInt(errorCodeText);
                } catch (NumberFormatException e) {
                    //do nothing
                }
            }
        }

        SMSActionResult out;
        switch (errorCode) {
            case 10:
            case 11:
            case 401:
                out = SMSActionResult.LOGIN_FAILED_ERROR();
                break;
            case 102:
                out = SMSActionResult.TIMEOUT_ERROR();
                break;
            case 88:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.not_enough_credits));
                break;
            case 33:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.unable_contact_carrier));
                break;
            case 12:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.invalid_country_code));
                break;
            case 8:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.invalid_sender));
                break;
            case -1:
            case 402:
            default:
                out = SMSActionResult.UNKNOWN_ERROR(response);
                out.setRetryMakesSense(false);
        }

        return out;
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
//        http://www.smsglobal.com/http-api.php?action=sendsms&user=sdsdsdsdsd&password=adsasdasd&to=<no_leadng_zeros>&from=<no_leading_zeros>&text=Hello_worldö
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
