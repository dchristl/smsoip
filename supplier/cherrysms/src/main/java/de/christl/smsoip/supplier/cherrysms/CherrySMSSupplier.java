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

package de.christl.smsoip.supplier.cherrysms;

import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Supplier for Cherry-SMS
 */
public class CherrySMSSupplier implements ExtendedSMSSupplier {


    private static final String TARGET_URL = "https://gw.cherry-sms.com/?user=%s&password=%s";

    private CherrySMSOptionProvider provider;
    private static final String ENCODING = "ISO-8859-1";

    public CherrySMSSupplier() {
        provider = new CherrySMSOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException {
        if (userName == null || password == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpUrl;
        try {
            tmpUrl = getURLStringWithUserNameAndPassword(userName, password);
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        HttpURLConnection httpURLConnection = factory.create();
        InputStream inputStream = httpURLConnection.getInputStream();

        String returnValue = UrlConnectionFactory.inputStream2DebugString(inputStream, ENCODING);
        if (returnValue.equals("")) {
            return SMSActionResult.NETWORK_ERROR();
        }
        int returnInt = Integer.parseInt(returnValue);
        if (returnInt == 10 || returnInt == 60) { //expect wrong receiver number, is better than check for credits
            return SMSActionResult.LOGIN_SUCCESSFUL();
        }
        SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_login_failed));
        smsActionResult.setRetryMakesSense(false);
        return smsActionResult;
    }

    private String getURLStringWithUserNameAndPassword(String userName, String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String md5Password = CherrySMSOptionProvider.getMD5String(password);
        return String.format(TARGET_URL, URLEncoder.encode(userName, ENCODING), md5Password);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
        if (!result.isSuccess()) {
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        String tmpUrl;
        try {
            tmpUrl = getURLStringWithUserNameAndPassword(provider.getUserName(), provider.getPassword());
            tmpUrl += "&message=" + URLEncoder.encode(smsText, ENCODING);
            int sendMethod = findSendMethod(spinnerText);
            int WITH_SI = 1;
            if (sendMethod == WITH_SI) {
                tmpUrl += "&from=1";
            }
        } catch (NoSuchAlgorithmException e) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
        }
        FireSMSResultList out = new FireSMSResultList(receivers.size());
        for (Receiver receiver : receivers) {
            UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl + "&to=" + receiver.getReceiverNumber(), UrlConnectionFactory.METHOD_GET);
            try {
                InputStream inputStream = factory.create().getInputStream();
                out.add(new FireSMSResult(receiver, processFireSMSReturn(inputStream)));
            } catch (SocketTimeoutException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }
        }
        return out;
    }

    private SMSActionResult processFireSMSReturn(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, ENCODING));
        String line;
        int i = 0;
        String[] split = new String[3];
        while ((line = reader.readLine()) != null) {
            split[i] = line;
            i++;
            if (i == 2) {   //we only need code and value
                break;
            }
        }
        try {
            int returnInt = 99;
            if (split.length > 0) {
                returnInt = Integer.parseInt(split[0]);
            }
            switch (returnInt) {
                case 10:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_10));
                case 20:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_20));
                case 30:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_30));
                case 31:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_31));
                case 40:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_40));
                case 50:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_50));
                case 60:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_60));
                case 70:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_70));
                case 71:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_71));
                case 80:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_80));
                case 90:
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_90));
                case 100:
                    String success = provider.getTextByResourceId(R.string.text_return_100);
                    if (split.length > 1) {
                        success = String.format(success, split[1]);
                    }
                    return SMSActionResult.NO_ERROR(success);
                default:
                    String unknownError = provider.getTextByResourceId(R.string.text_unknown_error);
                    unknownError = String.format(unknownError, returnInt);
                    return SMSActionResult.UNKNOWN_ERROR(unknownError);
            }
        } catch (NumberFormatException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR(split[0]);
        }


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

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException {
        return refreshInformations(true);
    }

    private SMSActionResult refreshInformations(boolean afterMessageSentSuccessful) throws IOException {
        if (!afterMessageSentSuccessful) {   //dont do a extra login if message is sent short time before
            SMSActionResult result = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!result.isSuccess()) {
                return result;
            }
        }

        String tmpText = provider.getTextByResourceId(R.string.text_refresh_informations);
        try {
            UrlConnectionFactory factory = new UrlConnectionFactory(getURLStringWithUserNameAndPassword(provider.getUserName(), provider.getPassword()) + "&check=guthaben", UrlConnectionFactory.METHOD_GET);
            HttpURLConnection httpURLConnection = factory.create();
            String returnValue = UrlConnectionFactory.inputStream2DebugString(httpURLConnection.getInputStream(), ENCODING);
            int returnInt = Integer.parseInt(returnValue);
            //only valid value here will be 50 (login failed, but will not reached here if creentials errous)
            return SMSActionResult.NO_ERROR(String.format(tmpText, returnInt));
        } catch (NoSuchAlgorithmException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR();
        }

    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        return refreshInformations(false);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
