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
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Innosend functionality
 */
public class InnosendSupplier implements ExtendedSMSSupplier, TimeShiftSupplier {

    private final InnosendOptionProvider provider;

    private static final int FREE = 0;
    private static final int SPEED = 1;
    private static final int POWER = 2;
    private static final int TURBO = 3;


    public static final String LOGIN_URL = "https://www.innosend.de/index.php?seite=login";
    public static final String INFO_URL = "https://www.innosend.de/index.php?seite=n_sms&art=free";
    public static final String GET_MOBILENUMBER_URL = "https://www.innosend.de/index.php?seite=bnaend";
    public static final String GATEWAY_URL = "https://www.innosend.de/gateway/";
    public static final String ACCOUNT_SUB = "konto.php?";
    public static final String SMS_SUB = "sms.php?";
    public static final String FREE_SUB = "free.php?app=1&was=iphone";

    private static final String ENCODING = "ISO-8859-1";

    private Long leaseTime = null;
    private String phpsessid;

    public InnosendSupplier() {
        provider = new InnosendOptionProvider();
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) {
        return fireTimeShiftSMS(smsText, receivers, spinnerText, null);
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) {
        if (userName == null || password == null) {
            leaseTime = null;
            phpsessid = null;
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpUrl;
        try {
            tmpUrl = GATEWAY_URL + ACCOUNT_SUB + "id=" + URLEncoder.encode(userName, ENCODING) + "&pw=" + URLEncoder.encode(password, ENCODING);
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
            leaseTime = null;
            phpsessid = null;
            //no floating point so check for error code
            int returnInt = returnValue.equals("") ? 0 : Integer.parseInt(returnValue);
            return getErrorMessageByResult(returnInt);
        } catch (SocketTimeoutException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.NETWORK_ERROR();
        } catch (NumberFormatException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }

    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() {
        return refreshInformations();
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() {
        return refreshInformations();
    }

    private synchronized SMSActionResult refreshInformations() {
        String tmpText = provider.getTextByResourceId(R.string.text_refresh_informations);
        try {
            SMSActionResult result = refreshSession();
            if (!result.isSuccess()) {
                return result;
            }

            UrlConnectionFactory infoFactory = new UrlConnectionFactory(INFO_URL, UrlConnectionFactory.METHOD_GET);
            List<String> cookies = new ArrayList<String>();
            cookies.add(phpsessid);
            infoFactory.setCookies(cookies);
            HttpURLConnection httpURLConnection = infoFactory.create();
            Document parse = Jsoup.parse(httpURLConnection.getInputStream(), ENCODING, "");
            Elements select = parse.select("div.modulecont div div p b");
            if (select.size() == 0) {
                return SMSActionResult.UNKNOWN_ERROR();
            }
            String freeSMS = "";
            for (Element element : select) {
                freeSMS = element.text();
                if (freeSMS.equals("SMS")) {
                    freeSMS = "0 " + freeSMS;
                }
            }
            Elements strongElements = parse.select("div.modulecont div div p strong");
            for (Element strongElement : strongElements) {
                String nextText = strongElement.text();
                if (nextText.contains(":") && !nextText.equals(":")) {
                    freeSMS += "\n" + String.format(provider.getTextByResourceId(R.string.text_next), nextText);
                    break;
                }
            }
            Elements balances = parse.select("ul#mainlevel-account li");
            String balance = "";
            for (Element element : balances) {
                if (element.text().contains("Guthaben:")) {
                    balance = element.text().replaceAll("Guthaben:", "").replaceAll("[^\\p{Print}]", "");
                    balance += " €";
                    break;
                }
            }

            return SMSActionResult.NO_ERROR(String.format(tmpText, freeSMS, balance));
        } catch (SocketTimeoutException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (UnsupportedEncodingException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.NETWORK_ERROR();
        } catch (NumberFormatException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR();
        }

    }

    /**
     * this can be called everytime, cause it checks if refresh is needed itself
     *
     * @return
     * @throws IOException
     */
    private SMSActionResult refreshSession() throws IOException {
        if (phpsessid == null || leaseTime == null || isSessionRefreshNeeded()) {
            SMSActionResult checkCredentialsResult = checkCredentials(provider.getUserName(), provider.getPassword());
            if (!checkCredentialsResult.isSuccess()) {
                leaseTime = null;
                phpsessid = null;
                return checkCredentialsResult;
            }
            UrlConnectionFactory loginFactory = new UrlConnectionFactory(LOGIN_URL);
            String userNamePasswordBody = "bn=" + URLEncoder.encode(provider.getUserName(), ENCODING) + "&pw=" + URLEncoder.encode(provider.getPassword(), ENCODING);
            HttpURLConnection con = loginFactory.writeBody(userNamePasswordBody);
            Map<String, List<String>> headerFields = con.getHeaderFields();
            if (headerFields == null || headerFields.size() == 0) {
                return SMSActionResult.LOGIN_FAILED_ERROR();
            }
            phpsessid = UrlConnectionFactory.findCookieByName(headerFields, "PHPSESSID");
            if (phpsessid == null || phpsessid.equals("")) {
                leaseTime = null;
                phpsessid = null;
                return SMSActionResult.LOGIN_FAILED_ERROR();
            }
            leaseTime = System.currentTimeMillis();
            provider.setAccountChanged(false);
        }
        return SMSActionResult.NO_ERROR();
    }

    private boolean isSessionRefreshNeeded() {
        boolean out = true;
        if (!provider.isAccountChanged()) {
            if (leaseTime != null) {
                out = leaseTime + (1 * 60 * 1000) - System.currentTimeMillis() < 0;
            }
        }
        return out;
    }

    private String getIdPwString() throws UnsupportedEncodingException {
        return "id=" + URLEncoder.encode(provider.getUserName(), ENCODING) + "&pw=" + URLEncoder.encode(provider.getPassword(), ENCODING);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }


    @Override
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) {
        int sendMethod = findSendMethod(spinnerText);
        Boolean isForeign = isForeign(receivers);
        if (isForeign == null) {
            String msg = provider.getTextByResourceId(R.string.text_mixing_not_allowed);
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(msg), receivers);
        }
        if (sendMethod == SPEED && isForeign && receivers.size() > 1) {
            String msg = provider.getTextByResourceId(R.string.text_multiple_receivers_not_allowed);
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(msg), receivers);
        }
        StringBuilder tmpUrl = new StringBuilder(GATEWAY_URL);
        if (sendMethod == FREE) {
            tmpUrl.append(FREE_SUB);
        } else {
            tmpUrl.append(SMS_SUB);
        }

        switch (sendMethod) {
            case FREE:
                break;//do nothing
            case TURBO:
                if (isForeign) {
                    tmpUrl.append("&type=8");
                } else {
                    tmpUrl.append("&type=4");
                }
                break;
            case SPEED:
                if (isForeign) {
                    tmpUrl.append("&type=10");
                } else {
                    tmpUrl.append("&type=2");
                }
                break;
            case POWER:
                tmpUrl.append("&type=3");

                break;
            default:
                return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
        }

        if (dateTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            tmpUrl.append("&termin=");
            tmpUrl.append(sdf.format(dateTime.getCalendar().getTime())).append("-");
            tmpUrl.append(String.format("%02d", dateTime.getHour())).append(":");
            tmpUrl.append(String.format("%02d", dateTime.getMinute()));
        }
        StringBuilder receiverListBuilder = new StringBuilder();
        for (int i = 0, receiversSize = receivers.size(); i < receiversSize; i++) {
            String receiverNumber = receivers.get(i).getReceiverNumber();
            receiverListBuilder.append(receiverNumber);
            if (i + 1 != receivers.size()) {
                receiverListBuilder.append(";");
            }
        }
        if (receivers.size() > 1) {
            tmpUrl.append("&massen=1");
        }
        try {
            if (dateTime != null || receivers.size() > 1) {
                String sender = findSenderAndWriteIfAvailable();
                if (sender.equals("")) {
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
                }
                tmpUrl.append("&absender=").append(URLEncoder.encode(sender, ENCODING));
            }
            String encodedText = URLEncoder.encode(smsText, ENCODING);
            if (encodedText.length() > 160) {
                tmpUrl.append("&maxi=1");
            }
            tmpUrl.append("&").append(getIdPwString()).append("&text=").append(encodedText).append("&empfaenger=").append(receiverListBuilder);
            UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl.toString(), UrlConnectionFactory.METHOD_GET);
            HttpURLConnection httpURLConnection = factory.create();
            String returnValue = UrlConnectionFactory.inputStream2DebugString(httpURLConnection.getInputStream(), ENCODING);
            int returnInt = Integer.parseInt(returnValue.replaceAll("\\D.*", ""));     //replace if there are some special chars behind, like the time in free sms
            return FireSMSResultList.getAllInOneResult(getErrorMessageByResult(returnInt), receivers);
        } catch (SocketTimeoutException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return FireSMSResultList.getAllInOneResult(SMSActionResult.TIMEOUT_ERROR(), receivers);
        } catch (UnsupportedEncodingException e) {
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return FireSMSResultList.getAllInOneResult(SMSActionResult.NETWORK_ERROR(), receivers);
        }
    }

    private Boolean isForeign(List<Receiver> receivers) {
        Boolean out = null;
        for (Receiver receiver : receivers) {
            if (receiver.getReceiverNumber().startsWith("0049")) {
                if (out == null) {
                    out = false;
                } else if (out) {
                    return null;
                }
            } else {
                if (out == null) {
                    out = true;
                } else if (!out) {
                    return null;
                }
            }
        }
        return out;
    }

    private String findSenderAndWriteIfAvailable() throws IOException {
        //first check in the options if sender is available
        String sender = provider.getSender();
        if (sender.equals("")) {
            refreshSession();
            UrlConnectionFactory infoFactory = new UrlConnectionFactory(GET_MOBILENUMBER_URL, UrlConnectionFactory.METHOD_GET);
            List<String> cookies = new ArrayList<String>();
            cookies.add(phpsessid);
            infoFactory.setCookies(cookies);
            HttpURLConnection httpURLConnection = infoFactory.create();
            Document parse = Jsoup.parse(httpURLConnection.getInputStream(), ENCODING, "");

            Elements tableColsWithBoldText = parse.select("table.contenttableform tr td:has(b)");
            if (tableColsWithBoldText.size() == 1) {//exactly one element with number should be found
                sender = tableColsWithBoldText.select("b").first().text();
                provider.writeSender(sender);
            }

        }
        return sender;
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
        return findSendMethod(spinnerText) != FREE;
    }

    private int findSendMethod(String spinnerText) {
        String[] arrayByResourceId = provider.getArrayByResourceId(R.array.array_spinner);
        int sendType = FREE;
        for (int i = 0, arrayByResourceIdLength = arrayByResourceId.length; i < arrayByResourceIdLength; i++) {
            String sendOption = arrayByResourceId[i];
            if (sendOption.equals(spinnerText)) {
                sendType = i;
            }
        }

        return sendType;
    }

    private SMSActionResult getErrorMessageByResult(int returnInt) {
        switch (returnInt) {
            case 100:
                return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.text_return_100));
            case 101:
                return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.text_return_101));
            case 111:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_111));
            case 112:
                SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_112));
                smsActionResult.setRetryMakesSense(false);
                return smsActionResult;
            case 120:
                provider.resetSender();
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_120));
            case 121:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_121));
            case 122:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_122));
            case 123:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_123));
            case 129:
                provider.resetSender();
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_129));
            case 130:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_130));
            case 134:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_134));
            case 140:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_140));
            case 150:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_150));
            case 161:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_161));
            case 162:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_162));
            case 170:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_170));
            case 171:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_171));
            case 172:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_172));
            default:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_return_unknown) + " " + returnInt);
        }
    }


}
