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

package de.christl.smsoip.supplier.innosend;

import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.List;

/**
 * Innosend functionality
 */
public class InnosendSupplier implements ExtendedSMSSupplier {

    private final InnosendOptionProvider provider;

    public static final String CHECK_URL = "https://www.innosend.de/gateway/konto.php?";
    private static final String ENCODING = "UTF-8";

    public InnosendSupplier() {
        provider = new InnosendOptionProvider();
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) {
        return null;
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) {
        if (userName == null || password == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpUrl;
        try {
            tmpUrl = CHECK_URL + "id=" + URLEncoder.encode(userName, ENCODING) + "&pw=" + URLEncoder.encode(password, ENCODING);
        } catch (UnsupportedEncodingException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        try {
            HttpURLConnection httpURLConnection = factory.create();
            InputStream inputStream = httpURLConnection.getInputStream();

            String returnValue = UrlConnectionFactory.inputStream2DebugString(inputStream, ENCODING);
            if (returnValue.contains(",")) { //its floating point number so credits will be replied
                return SMSActionResult.LOGIN_SUCCESSFUL();
            }
            //no floating point so check for error code
            int returnInt = Integer.parseInt(returnValue);
            return SMSActionResult.UNKNOWN_ERROR(getErrorMessageByResult(returnInt));
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.NETWORK_ERROR();
        } catch (NumberFormatException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }

    }

    private String getErrorMessageByResult(int returnInt) {
        switch (returnInt) {
            case 100:
                return provider.getTextByResourceId(R.string.text_return_100);
            case 101:
                return provider.getTextByResourceId(R.string.text_return_101);
            case 111:
                return provider.getTextByResourceId(R.string.text_return_111);
            case 112:
                return provider.getTextByResourceId(R.string.text_return_112);
            case 120:
                return provider.getTextByResourceId(R.string.text_return_120);
            case 121:
                return provider.getTextByResourceId(R.string.text_return_121);
            case 122:
                return provider.getTextByResourceId(R.string.text_return_122);
            case 123:
                return provider.getTextByResourceId(R.string.text_return_123);
            case 129:
                return provider.getTextByResourceId(R.string.text_return_129);
            case 130:
                return provider.getTextByResourceId(R.string.text_return_130);
            default:
                return provider.getTextByResourceId(R.string.text_return_unknown) + " " + returnInt;
        }
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() {
        return null;
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() {
        return null;
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
