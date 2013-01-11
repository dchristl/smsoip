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

package de.christl.smsoip.supplier.sms77;


import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.List;

public class SMS77Supplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private OptionProvider provider;

    private static final String ENCODING = "ISO-8859-1";
    private static final String API_URL = "https://gateway.sms77.de/";
    private static final String BALANCE_URL = API_URL + "balance.php?";
    private static final String USER_PASS = "u=%s&p=%s";

    private static final int SEND_BASIC = 0;
    private static final int SEND_QUALITY = 1;
    private static final int SEND_LANDLINE = 2;
    private static final int SEND_FLASH = 3;


    public SMS77Supplier() {
        provider = new SMS77OptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        String tmpUrl;
        try {
            tmpUrl = BALANCE_URL + getURLStringWithUserNameAndPassword(userName, password);
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        InputStream httpURLConnection = factory.create().getInputStream();
        String returnVal = UrlConnectionFactory.inputStream2DebugString(httpURLConnection);
        if (returnVal.contains(".")) {
            return SMSActionResult.LOGIN_SUCCESSFUL();
        } else {
            int returnCode;
            try {
                returnCode = Integer.parseInt(returnVal);
                if (returnCode == 0) {                        //return succesful if balance is Zero
                    return SMSActionResult.LOGIN_SUCCESSFUL();
                }
            } catch (NumberFormatException e) {
                return SMSActionResult.UNKNOWN_ERROR(returnVal);
            }
            return translateCodeToActionResult(returnCode);
        }

    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInfoTextAfterMessageSuccessfulSent();
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        String tmpUrl;
        try {
            tmpUrl = BALANCE_URL + getURLStringWithUserNameAndPassword(provider.getUserName(), provider.getPassword());
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
        InputStream httpURLConnection = factory.create().getInputStream();
        String returnVal = UrlConnectionFactory.inputStream2DebugString(httpURLConnection);
        if (returnVal.contains(".")) {
            return SMSActionResult.NO_ERROR(String.format(provider.getTextByResourceId(R.string.balance), returnVal));
        } else {
            int returnCode;
            try {
                returnCode = Integer.parseInt(returnVal);
                if (returnCode == 0) {                        //return succesful if balance is Zero
                    return SMSActionResult.NO_ERROR(String.format(provider.getTextByResourceId(R.string.balance), returnVal));
                }
            } catch (NumberFormatException e) {
                return SMSActionResult.UNKNOWN_ERROR(returnVal);
            }
            return translateCodeToActionResult(returnCode);
        }
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        return fireTimeShiftSMS(smsText, receivers, spinnerText, null);
    }

    private String getURLStringWithUserNameAndPassword(String userName, String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String md5Password = UrlConnectionFactory.getMD5String(password, ENCODING);
        return String.format(USER_PASS, URLEncoder.encode(userName, ENCODING), md5Password);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }


    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException, NumberFormatException {
        StringBuilder urlBuilder = new StringBuilder();
        try {
            urlBuilder.append(API_URL).append("?").append(getURLStringWithUserNameAndPassword(provider.getUserName(), provider.getPassword()));
            urlBuilder.append("&debug=1");
        } catch (NoSuchAlgorithmException e) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
        }
        int sendMethod = findSendMethod(spinnerText);
        urlBuilder.append("&type=");
        switch (sendMethod) {
            default:
            case SEND_BASIC:
                urlBuilder.append("basicplus");
                break;
            case SEND_QUALITY:
                urlBuilder.append("quality");
                break;
            case SEND_LANDLINE:
                urlBuilder.append("festnetz");
                break;
            case SEND_FLASH:
                urlBuilder.append("flash");
                break;
        }
        if (dateTime != null) {
            urlBuilder.append("&delay=");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd%20HH:mm:00");
            urlBuilder.append(sdf.format(dateTime.getCalendar().getTime()));
        }

        urlBuilder.append("&text=").append(URLEncoder.encode(smsText, ENCODING));

        FireSMSResultList out = new FireSMSResultList();
        for (Receiver receiver : receivers) {
            String url = urlBuilder.toString() + "&to=" + receiver.getReceiverNumber();
            UrlConnectionFactory factory = new UrlConnectionFactory(url);
            InputStream httpURLConnection = factory.create().getInputStream();
            String returnVal = UrlConnectionFactory.inputStream2DebugString(httpURLConnection);
            try {
                int returnCode = Integer.parseInt(returnVal);
                SMSActionResult smsActionResult = translateCodeToActionResult(returnCode);
                out.add(new FireSMSResult(receiver, smsActionResult));
            } catch (NumberFormatException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR(returnVal)));
            }

        }
        return out;
//        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl);
//        InputStream httpURLConnection = factory.create().getInputStream();
//        String returnVal = UrlConnectionFactory.inputStream2DebugString(httpURLConnection);
//        if (returnVal.contains(".")) {
//            return SMSActionResult.NO_ERROR(String.format(provider.getTextByResourceId(R.string.balance), returnVal));
//        } else {
//            int returnCode;
//            try {
//                returnCode = Integer.parseInt(returnVal);
//                if (returnCode == 0) {                        //return succesful if balance is Zero
//                    return SMSActionResult.NO_ERROR(String.format(provider.getTextByResourceId(R.string.balance), returnVal));
//                }
//            } catch (NumberFormatException e) {
//                return SMSActionResult.UNKNOWN_ERROR(returnVal);
//            }
//            return translateCodeToActionResult(returnCode);
//        }
//        return null;
    }

    private SMSActionResult translateCodeToActionResult(int code) throws IOException {
        switch (code) {
            case 100:
                return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_100));
            case 101:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_101));
            case 201:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_201));
            case 202:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_202));
            case 300:
                SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_300));
                smsActionResult.setRetryMakesSense(false);
                return smsActionResult;
            case 301:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_301));
            case 304:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_304));
            case 305:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_305));
            case 306:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_306));
            case 400:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_400));
            case 401:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_401));
            case 402:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_402));
            case 500:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_500));
            case 600:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_600));
            case 801:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_801));
            case 802:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_802));
            case 803:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_803));
            case 900:
                SMSActionResult loginFailedError = SMSActionResult.LOGIN_FAILED_ERROR();
                loginFailedError.setMessage(provider.getTextByResourceId(R.string.return_900));
                return loginFailedError;
            case 902:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_902));
            case 903:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_903));
            default:
                String unknownError = provider.getTextByResourceId(R.string.return_unknown_error);
                unknownError = String.format(unknownError, code);
                return SMSActionResult.UNKNOWN_ERROR(unknownError);
        }
    }

    @Override
    public int getMinuteStepSize() {
        return 1;
    }

    @Override
    public int getDaysInFuture() {
        return 365 * 5; //five years should be enough
    }

    @Override
    public boolean isSendTypeTimeShiftCapable(String spinnerText) {
        return true;
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = SEND_BASIC;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i;
            }
        }

        return sendType;
    }
}
