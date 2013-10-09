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
import java.text.SimpleDateFormat;
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

    private static final String LOGIN_BALANCE_URL = "http://www.smsglobal.com/http-api.php?user=%s&password=%s&action=balancesms";
    private static final String SEND_URL = "http://www.smsglobal.com/http-api.php?action=sendsms&user=%s&password=%s&to=%s&from=%s&text=%s";
    private static final String ENCODING = "UTF-8";


    private final SMSGlobalOptionProvider provider;
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("Error[:]? ([0-9]+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_PATTERN = Pattern.compile("Error (.*)", Pattern.CASE_INSENSITIVE);

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
        String userName = provider.getUserName();
        String password = provider.getPassword();
        if (password == null || userName == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpUrl = String.format(LOGIN_BALANCE_URL, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING));
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
        Matcher m = ERROR_CODE_PATTERN.matcher(response);
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
        if (errorCode == -1) {
            m = ERROR_PATTERN.matcher(response);
            if (m.matches()) {
                String errorCodeText = m.group(1);
                if (errorCodeText != null) {
                    return SMSActionResult.UNKNOWN_ERROR(errorCodeText);
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
            case 102:
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
        String userName = provider.getUserName();
        String password = provider.getPassword();
        if (password == null || userName == null) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.LOGIN_FAILED_ERROR(), receivers);
        }

        String from = provider.getSender();
        if (from == null || from.trim().length() == 0) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.invalid_sender_check)), receivers);
        }
        StringBuilder receiverString = new StringBuilder();
        int i = 1;
        for (Receiver receiver : receivers) {
            String shortReceiverNumber = receiver.getReceiverNumber().replaceAll("^00", "");
            receiverString.append(shortReceiverNumber);
            if (i < receivers.size()) {
                receiverString.append(",");
            }
            i++;

        }
        smsText = URLEncoder.encode(smsText, ENCODING);
        String tmpUrl = String.format(SEND_URL, URLEncoder.encode(userName, ENCODING), URLEncoder.encode(password, ENCODING), receiverString, from, smsText);
        if (dateTime != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
            tmpUrl += "&scheduledatetime=" + URLEncoder.encode(df.format(dateTime.getCalendar().getTime()), ENCODING);
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        InputStream inputStream = factory.create().getInputStream();
        if (inputStream == null) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.NETWORK_ERROR(), receivers);
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream);
        SMSActionResult smsActionResult = parseSendResponse(response, dateTime == null);
        if (smsActionResult.isSuccess()) {
            provider.saveLastSender();
        }
        return FireSMSResultList.getAllInOneResult(smsActionResult, receivers);
    }

    private SMSActionResult parseSendResponse(String response, boolean noTimeshift) {
        SMSActionResult out;
        if ((noTimeshift && response.contains("OK: 0")) || (!noTimeshift && response.contains("SMSGLOBAL DELAY"))) {  //OK: 0; Sent queued message ID: 941596             SMSGlobalMsgID:6764842339385521
            out = SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.sentSuccess));
        } else { //SMSGLOBAL DELAY MSGID:19736759
            out = parseErrorResponse(response);
        }
        return out;
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
