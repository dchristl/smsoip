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
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class SMSFlatrateSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {
    private SMSFlatrateOptionProvider provider;


    private static final String API_BASE = "http://www.smsflatrate.net/appkey.php?appkey=%s&lizenz=217075022&aid=4949&";
    private static final String INFO_TEXT_URL = API_BASE + "request=credits";
    private static final String SEND_URL = API_BASE + "text=%s&type=%d&from=%s";

    //    GATEWAYS
    private static final int SMART_DE = 20;
    private static final int SMART_INT = 21;
    private static final int NORMAL_WO_SI_DE = 3;
    private static final int NORMAL_WO_SI_INT = 4;
    private static final int NORMAL_SI_DE = 1;
    private static final int NORMAL_SI_INT = 2;
    private static final int QUALITY_SI_DE = 10;
    private static final int QUALITY_SI_INT = 11;

    //    SENDING TYPES
    private static final int SMART = 0;
    private static final int NORMAL_WO_SI = 1;
    private static final int NORMAL_SI = 2;
    private static final int QUALITY = 3;


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
        int sendMethod = findSendMethod(spinnerText);
        switch (sendMethod) {
            case QUALITY:
            case NORMAL_SI:
                String sender = provider.getSender();
                if (sender.trim().equals("")) {
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.invalidSender)), receivers);
                }
                break;


        }
        List<Receiver> foreignReceivers = findForeignReceivers(receivers);
        List<Receiver> domesticReceivers = findDomesticReceivers(receivers);
        FireSMSResultList out = new FireSMSResultList(receivers.size());
        smsText = URLEncoder.encode(smsText, ENCODING);
        if (sendMethod == NORMAL_WO_SI && smsText.length() > 160) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.textTooLong)), receivers);
        }
        String formattedDate = null;
        if (dateTime != null) {
//            TT.MM.JJJJ-SS:MM
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy-HH:mm");
            formattedDate = format.format(dateTime.getCalendar().getTime());
        }
        if (domesticReceivers.size() > 0) {

            out.addAll(sendDomestic(sendMethod, domesticReceivers, smsText, formattedDate));
        }
        if (foreignReceivers.size() > 0) {
            out.addAll(sendForeign(sendMethod, foreignReceivers, smsText, formattedDate));
        }

        FireSMSResultList outCopy = new FireSMSResultList(out.size());
        //copy results to force updating result
        for (FireSMSResult fireSMSResult : out) {
            outCopy.add(fireSMSResult);
        }
        if (outCopy.getResult() == FireSMSResultList.SendResult.SUCCESS) {
            provider.saveLastSender();
        }
        return outCopy;
    }

    private List<FireSMSResult> sendForeign(int sendMethod, List<Receiver> foreignReceivers, String smsText, String dateTime) throws UnsupportedEncodingException {
        if (sendMethod == SMART) {
            if (smsText.length() > 160) {
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.smartForeign)), foreignReceivers);
            }
            if (dateTime != null) {
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.smartForeignDelay)), foreignReceivers);
            }
        }
        FireSMSResultList out;

        int gateway = findGateway(sendMethod, smsText, true);
        String sender = sendMethod == SMART || sendMethod == NORMAL_WO_SI ? "" : URLEncoder.encode(provider.getSender(), ENCODING);
        String tmpUrl = String.format(SEND_URL, URLEncoder.encode(provider.getPassword(), ENCODING), smsText, gateway, sender);
        if (sendMethod == QUALITY || sendMethod == NORMAL_SI) {
            tmpUrl += "&status=1";
        }
        if (dateTime != null) {
            tmpUrl += "&time=" + dateTime;
        }
        if (gateway == SMART_INT) {
            out = sendBulkOneByOne(tmpUrl, foreignReceivers);
        } else {
            out = sendBulk(foreignReceivers, tmpUrl);
        }

        return out;
    }

    private FireSMSResultList sendBulk(List<Receiver> receivers, String tmpUrl) {
        FireSMSResultList out;
        tmpUrl += "&to=" + splitSenders(receivers);
        if (receivers.size() > 1) {
            tmpUrl += "&bulk=1";
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        try {
            InputStream inputStream = factory.getConnnection().getInputStream();
            out = FireSMSResultList.getAllInOneResult(getResultByResponse(inputStream), receivers);
        } catch (SocketTimeoutException e) {
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.TIMEOUT_ERROR(), receivers);
        } catch (IOException e) {
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.NETWORK_ERROR(), receivers);
        }
        return out;
    }

    private FireSMSResultList sendBulkOneByOne(String tmpUrl, List<Receiver> foreignReceivers) {
        FireSMSResultList out = new FireSMSResultList(foreignReceivers.size());
        for (Receiver foreignReceiver : foreignReceivers) {
            tmpUrl += "&to=" + foreignReceiver.getReceiverNumber();
            UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
            try {
                InputStream inputStream = factory.getConnnection().getInputStream();
                out.add(new FireSMSResult(foreignReceiver, getResultByResponse(inputStream)));
            } catch (SocketTimeoutException e) {
                out.add(new FireSMSResult(foreignReceiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                out.add(new FireSMSResult(foreignReceiver, SMSActionResult.NETWORK_ERROR()));
            }
        }
        return out;
    }

    private SMSActionResult getResultByResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return SMSActionResult.NETWORK_ERROR();
        }
        String response = UrlConnectionFactory.inputStream2DebugString(inputStream).replaceAll(",.*",""); //if with status is sent response + comma and id is appended
        int responseCode;
        try {
            responseCode = Integer.parseInt(response);
        } catch (NumberFormatException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        switch (responseCode) {
            case 100:
                return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_100));
            case 104:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_104));
            case 120:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_120));
            case 130:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_130));
            case 131:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_131));
            case 132:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_132));
            case 133:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_133));
            case 140:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_140));
            case 150:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_150));
            case 170:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_170));
            case 171:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_171));
            case 231:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_231));
            case 404:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_404));
            default:
                return SMSActionResult.UNKNOWN_ERROR();
        }

    }

    private String splitSenders(List<Receiver> receiverList) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < receiverList.size(); i++) {
            Receiver receiver = receiverList.get(i);
            out.append(receiver.getReceiverNumber());
            if (i < receiverList.size() - 1) {
                out.append(";");
            }
        }
        return out.toString();
    }

    private List<FireSMSResult> sendDomestic(int sendMethod, List<Receiver> domesticReceivers, String smsText, String dateTime) throws UnsupportedEncodingException {
        int gateway = findGateway(sendMethod, smsText, false);
        String sender = sendMethod == SMART || sendMethod == NORMAL_WO_SI ? "" : URLEncoder.encode(provider.getSender(), ENCODING);
        String tmpUrl = String.format(SEND_URL, URLEncoder.encode(provider.getPassword(), ENCODING), smsText, gateway, sender);
        if (sendMethod == QUALITY || sendMethod == NORMAL_SI) {
            tmpUrl += "&status=1";
        }
        if (dateTime != null) {
            tmpUrl += "&time=" + dateTime;
        }

        return sendBulk(domesticReceivers, tmpUrl);
    }

    private int findGateway(int sendMethod, String smsText, boolean isForeign) {
        int smsLength = smsText.length();
        int out = -1;
        switch (sendMethod) {
            case SMART:
                if (smsLength <= 160) {
                    if (isForeign) {
                        out = SMART_INT;
                    } else {
                        out = SMART_DE;
                    }
                } else if (!isForeign) {
                    out = SMART_DE;
                    //foreign returns -1 = error
                }
                break;
            case NORMAL_WO_SI:
                if (smsLength <= 160) {
                    if (isForeign) {
                        out = NORMAL_WO_SI_INT;
                    } else {
                        out = NORMAL_WO_SI_DE;
                    }
                }
                break;
            case NORMAL_SI:
                if (isForeign) {
                    out = NORMAL_SI_INT;
                } else {
                    out = NORMAL_SI_DE;
                }
                break;
            case QUALITY:
                if (isForeign) {
                    out = QUALITY_SI_INT;
                } else {
                    out = QUALITY_SI_DE;
                }
                break;
        }
        return out;
    }

    private List<Receiver> findDomesticReceivers(List<Receiver> receivers) {
        List<Receiver> out = new ArrayList<Receiver>();
        for (Receiver receiver : receivers) {
            if (receiver.getReceiverNumber().startsWith("0049")) {
                out.add(receiver);
            }
        }
        return out;
    }

    private List<Receiver> findForeignReceivers(List<Receiver> receivers) {
        List<Receiver> out = new ArrayList<Receiver>();
        for (Receiver receiver : receivers) {
            if (!receiver.getReceiverNumber().startsWith("0049")) {
                out.add(receiver);
            }
        }
        return out;
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = SMART;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i;
            }
        }

        return sendType;
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
