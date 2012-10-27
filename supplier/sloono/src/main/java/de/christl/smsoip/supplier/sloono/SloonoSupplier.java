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

package de.christl.smsoip.supplier.sloono;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;


public class SloonoSupplier implements TimeShiftSupplier, ExtendedSMSSupplier {
    SloonoOptionProvider provider;

    private static final String ENCODING = "UTF-8";

    private static final String LOGIN_BALANCE_URL = "http://www.sloono.de/API/httpkonto.php?return=xml&";
    private static final String SEND_URL = "http://www.sloono.de/API/httpsms.php?return=xml&action=info&";
    private static final int BASIC = 1;
    private static final int PRO = 2;

    public SloonoSupplier() {
        provider = new SloonoOptionProvider(this);
    }


    @Override
    public synchronized SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {
        String tmpUrl;
        try {
            tmpUrl = LOGIN_BALANCE_URL + "user=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&password=" + getMD5String(password == null ? "" : password);
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        HttpURLConnection httpURLConnection = factory.create();
        Document parse = Jsoup.parse(httpURLConnection.getInputStream(), ENCODING, "", Parser.xmlParser());
        try {
            int returnCode = Integer.parseInt(parse.select("answer code").text());
            if (returnCode == 101) {
                return SMSActionResult.LOGIN_SUCCESSFUL();
            } else {
                return translateReturnCodeToSMSActionResult(returnCode);
            }
        } catch (NumberFormatException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }

    }


    private SMSActionResult translateReturnCodeToSMSActionResult(int returnCode) {
        SMSActionResult out;
        switch (returnCode) {
            case 100:
                out = SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_100));
                break;
            case 101:
                out = SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_101));
                break;
            case 102:
                out = SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_102));
                break;
            case 200:
                out = SMSActionResult.LOGIN_FAILED_ERROR();
                out.setMessage(provider.getTextByResourceId(R.string.return_200));
                break;
            case 201:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_201));
                break;
            case 202:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_202));
                break;
            case 203:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_203));
                break;
            case 204:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_204));
                break;
            case 205:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_205));
                break;
            case 206:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_206));
                break;
            case 207:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_207));
                break;
            case 208:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_208));
                break;
            case 300:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_300));
                break;
            case 301:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_301));
                break;
            case 302:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_302));
                break;
            case 400:
                out = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_400));
                out.setRetryMakesSense(false);
                break;
            default:
                String returnText = String.format(provider.getTextByResourceId(R.string.return_unknown), returnCode);
                out = SMSActionResult.UNKNOWN_ERROR(returnText);
                break;
        }
        return out;
    }


    @Override
    public synchronized SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return refreshInfoTextAfterMessageSuccessfulSent();
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        String tmpUrl;
        try {
            String userName = provider.getUserName();
            String password = provider.getPassword();
            tmpUrl = LOGIN_BALANCE_URL + "user=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&password=" + getMD5String(password == null ? "" : password);
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        HttpURLConnection httpURLConnection = factory.create();
        Document parse = Jsoup.parse(httpURLConnection.getInputStream(), ENCODING, "", Parser.xmlParser());
        try {
            int returnCode = Integer.parseInt(parse.select("answer code").text());
            if (returnCode == 101) {
                String balance = parse.select("answer info kontostand").text();
                String message = String.format(provider.getTextByResourceId(R.string.balance), balance);
                return SMSActionResult.NO_ERROR(message);
            } else {
                return translateReturnCodeToSMSActionResult(returnCode);
            }
        } catch (NumberFormatException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
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
        FireSMSResultList out = new FireSMSResultList(receivers.size());
        for (Receiver receiver : receivers) {
            StringBuilder tmpUrl = new StringBuilder();
            try {
                String userName = provider.getUserName();
                String password = provider.getPassword();
                tmpUrl.append(SEND_URL + "user=").append(URLEncoder.encode(userName == null ? "" : userName, ENCODING)).append("&password=").append(getMD5String(password == null ? "" : password));
            } catch (NoSuchAlgorithmException e) {
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
            }

            int sendMethod = findSendMethod(spinnerText);
            tmpUrl.append("&typ=").append(sendMethod);
            String time = "0";
            if (dateTime != null) {
                time = String.valueOf(dateTime.getCalendar().getTimeInMillis() / 1000L);
            }
            tmpUrl.append("&timestamp=").append(time);

            tmpUrl.append("&text=").append(URLEncoder.encode(smsText, ENCODING));
            if (sendMethod == PRO) {
                String sender = provider.getSender();
                if (sender == null) {
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_refresh_sender_first)), receivers);
                }
                tmpUrl.append("&from=").append(sender);
            }
            String toString = receiver.getReceiverNumber();
            tmpUrl.append("&to=").append(URLEncoder.encode(toString, ENCODING));
            UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl.toString(), UrlConnectionFactory.METHOD_GET);
            try {
                HttpURLConnection httpURLConnection = factory.create();
                Document parse = Jsoup.parse(httpURLConnection.getInputStream(), ENCODING, "", Parser.xmlParser());
                try {
                    int returnCode = Integer.parseInt(parse.select("answer code").text());
                    if (returnCode == 101) {
                        SMSActionResult result = resolveResult(parse);
                        out.add(new FireSMSResult(receiver, result));
                    } else {
                        out.add(new FireSMSResult(receiver, translateReturnCodeToSMSActionResult(returnCode)));

                    }
                } catch (NumberFormatException e) {
                    out.add(new FireSMSResult(receiver, SMSActionResult.UNKNOWN_ERROR()));
                }
            } catch (SocketTimeoutException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.TIMEOUT_ERROR()));
            } catch (IOException e) {
                out.add(new FireSMSResult(receiver, SMSActionResult.NETWORK_ERROR()));
            }
        }
        return out;
    }

    private SMSActionResult resolveResult(Document parse) {
        String resultTextRaw = provider.getTextByResourceId(R.string.result);
        String smsCount = parse.select("answer info sms").text();
        String target = parse.select("answer info ziel").text();
        String costs = parse.select("answer info kosten").text();

        return SMSActionResult.UNKNOWN_ERROR(String.format(resultTextRaw, smsCount, target, costs));
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = BASIC;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i + 1;
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
        return 365;
    }

    @Override
    public boolean isSendTypeTimeShiftCapable(String spinnerText) {
        return true;
    }

    static String getMD5String(String utf8String) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessage = utf8String.getBytes("ISO-8859-1");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] thedigest = md.digest(bytesOfMessage);
        StringBuilder hexString = new StringBuilder();
        for (byte aThedigest : thedigest) {
            String hexStringRaw = Integer.toHexString(0xFF & aThedigest);
            hexString.append(("00" + hexStringRaw).substring(hexStringRaw.length()));   //add leading zero to String
        }

        return hexString.toString();
    }

    public SMSActionResult resolveNumbers() throws IOException {
        String tmpUrl;
        try {
            String userName = provider.getUserName();
            String password = provider.getPassword();
            tmpUrl = LOGIN_BALANCE_URL + "user=" + URLEncoder.encode(userName == null ? "" : userName, ENCODING) + "&password=" + getMD5String(password == null ? "" : password);
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        HttpURLConnection httpURLConnection = factory.create();
        Document parse = Jsoup.parse(httpURLConnection.getInputStream(), ENCODING, "", Parser.xmlParser());
        try {
            int returnCode = Integer.parseInt(parse.select("answer code").text());
            if (returnCode == 101) {
                Elements senders = parse.select("answer info absender");
                if (senders.size() != 1) {
                    return SMSActionResult.UNKNOWN_ERROR();
                }
                HashMap<Integer, String> numberMap = new HashMap<Integer, String>(4);
                for (Element sender : senders.get(0).children()) {
                    Tag tag = sender.tag();
                    if (tag.getName().startsWith("kennung") && !sender.text().equals("")) {
                        numberMap.put(Integer.parseInt(tag.getName().replace("kennung", "")) + 1, sender.text());
                    }
                }
                if (numberMap.size() > 0) {
                    provider.saveSenders(numberMap);
                    return SMSActionResult.NO_ERROR();
                } else {
                    return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.no_numbers_maintened));
                }
            } else {
                return translateReturnCodeToSMSActionResult(returnCode);
            }
        } catch (NumberFormatException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }
    }
}
