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

package de.christl.smsoip.supplier.directbox;

import android.os.Build;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static de.christl.smsoip.supplier.directbox.ErrorConstants.*;


public class DirectboxSupplier implements ExtendedSMSSupplier {


    private DirectboxOptionProvider provider;
    private static final String TOKEN_URL = "https://api.xworks.net/xpad/XpadJsonService.svc/GetCustomerToken";
    private static final String TOKEN_REQUEST = "{\"request\":{\"client\":{\"device\":\"MOBILE-APP\",\"validdays\":365,\"info\":\"%s\"},\"credentials\":{\"user\":\"%s\",\"pass\":\"%s\"}}}";

    /*JSON TOKEN*/
    private static final String JSON_ERROR = "error";
    private static final String JSON_TOKEN = "token";
    private static final String JSON_MESSAGE = "message";
    private static final String GET_CUSTOMER_TOKEN_RESULT = "GetCustomerTokenResult";

    public DirectboxSupplier() {
        provider = new DirectboxOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        UrlConnectionFactory factory = new UrlConnectionFactory(TOKEN_URL, UrlConnectionFactory.METHOD_POST);
        String device = Build.MODEL;
/*
CODE FOR NEW FACTORY if deployed
       Map<String, String> requestProperties = new HashMap<String, String>();
        requestProperties.put("Content-Type", "application/json; charset=utf-8");
        requestProperties.put("X-Requested-With", "XMLHttpRequest");
        factory.setRequestProperties(requestProperties);
        HttpURLConnection httpURLConnection = factory.writeBody(String.format(TOKEN_REQUEST, device, userName, password));
         String s = UrlConnectionFactory.inputStream2DebugString(httpURLConnection.getInputStream());
        */
        /*START OF TO DELETE CODE*/
        HttpURLConnection con = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
        writer.write(String.format(TOKEN_REQUEST, device, userName, password));
        writer.flush();
        writer.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
        String line;
        StringBuilder returnFromServer = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            returnFromServer.append(line);
        }
        /*END OF TO DELETE CODE*/

        String returnS = returnFromServer.toString();
        if (returnS.equals("") || !returnS.contains(GET_CUSTOMER_TOKEN_RESULT)) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        String error = getJSONContent(returnS, JSON_ERROR);
        if (!error.equals("null")) {

            return translateErrroMessageToResult(getJSONContent(returnS, JSON_MESSAGE));
//            {"GetCustomerTokenResult":{"error":{"code":"E_UNKNOWN_ERROR","message":"Object reference not set to an instance of an object."},"state":1,"token":null}}
        }

//        {"GetCustomerTokenResult":{"error":null,"state":0,"token":"4gfgfg27a"}}
        String token = getJSONContent(returnS, JSON_TOKEN);
        token = token.replaceAll("[^0-9a-fA-F]", "");      //replace all non alphanum
        if (token.equals("")) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        provider.saveToken(userName, token);
        return SMSActionResult.LOGIN_SUCCESSFUL();
    }

    private SMSActionResult translateErrroMessageToResult(String jsonContent) {
        if (jsonContent.equals(OBJECT_REFERENCE_NOT_SET_TO_AN_INSTANCE_OF_AN_OBJECT)) {
            return SMSActionResult.UNKNOWN_ERROR();
        } else if (jsonContent.equals(LOGIN_FAILURE_BAD_USERNAME_OR_PASSWORD)) {
            SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_login_error));
            smsActionResult.setRetryMakesSense(false);
            return smsActionResult;
        }
        return SMSActionResult.UNKNOWN_ERROR(jsonContent);
    }

    private static String getJSONContent(String jsonString, String token) {
        String content = jsonString.replaceAll(".*\"" + token + "\":", "");
        content = content.replaceAll(",.*", "");
        return content;
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
