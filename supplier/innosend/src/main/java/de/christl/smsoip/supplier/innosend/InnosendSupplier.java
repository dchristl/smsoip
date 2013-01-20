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
    private static final int LANDLINE = 4;


    public static final String LOGIN_URL = "https://www.innosend.de/index.php?seite=login";
    public static final String INFO_URL = "https://www.innosend.de/index.php?seite=n_sms&art=free";
    public static final String GET_MOBILENUMBER_URL = "https://www.innosend.de/index.php?seite=bnaend";
    public static final String GATEWAY_URL = "https://www.innosend.de/gateway/";
    public static final String ACCOUNT_SUB = "konto.php?";
    public static final String SMS_SUB = "sms.php?";
    public static final String LANDLINE_SUB = "fest.php?";
    public static final String FREE_SUB = "free.php?app=1&was=iphone";

    private static final String ENCODING = "ISO-8859-1";

    private Long leaseTime = null;
    private String phpsessid;

    public InnosendSupplier() {
        provider = new InnosendOptionProvider();
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException {
        return fireTimeShiftSMS(smsText, receivers, spinnerText, null);
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException {
        if (userName == null || password == null) {
            leaseTime = null;
            phpsessid = null;
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpUrl = GATEWAY_URL + ACCOUNT_SUB + "id=" + URLEncoder.encode(userName, ENCODING) + "&pw=" + URLEncoder.encode(password, ENCODING);

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
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

    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException {
        return refreshInformations();
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException {
        return refreshInformations();
    }

    private synchronized SMSActionResult refreshInformations() throws IOException {
        SMSActionResult result = refreshSession();
        if (!result.isSuccess()) {
            return result;
        }
        String tmpText = provider.getTextByResourceId(R.string.refresh_informations);

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
        StringBuilder freeSMS = new StringBuilder();
        for (Element element : select) {
            freeSMS.append(element.text());
            if (freeSMS.toString().equals("SMS")) {
                freeSMS = new StringBuilder("0 ");
                freeSMS.append(freeSMS);
            }
        }
        Elements strongElements = parse.select("div.modulecont div div p strong");
        for (Element strongElement : strongElements) {
            String nextText = strongElement.text();
            if (nextText.contains(":") && !nextText.equals(":")) {
                freeSMS.append("\n").append(String.format(provider.getTextByResourceId(R.string.next), nextText));
                break;
            }
        }
        Elements balances = parse.select("ul#mainlevel-account li");
        StringBuilder balance = new StringBuilder();
        for (Element element : balances) {
            if (element.text().contains("Guthaben:")) {
                balance = new StringBuilder(element.text().replaceAll("Guthaben:", "").replaceAll("[^\\p{Print}]", ""));
                balance.append(" €");
                break;
            }
        }

        return SMSActionResult.NO_ERROR(String.format(tmpText, freeSMS, balance.toString()));

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
    public FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException {
        SMSActionResult result = refreshSession();
        if (!result.isSuccess()) {
            provider.saveState();
            return FireSMSResultList.getAllInOneResult(result, receivers);
        }
        int sendMethod = findSendMethod(spinnerText);
        Boolean isForeign = isForeign(receivers);
        if (isForeign == null) {
            String msg = provider.getTextByResourceId(R.string.mixing_not_allowed);
            provider.saveState();
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(msg), receivers);
        }
        if (sendMethod == SPEED && isForeign && receivers.size() > 1) {
            String msg = provider.getTextByResourceId(R.string.multiple_receivers_not_allowed);
            provider.saveState();
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(msg), receivers);
        }
        if (sendMethod == LANDLINE && isForeign) {
            String msg = provider.getTextByResourceId(R.string.landline_outside_germany);
            provider.saveState();
            return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(msg), receivers);
        }
        StringBuilder tmpUrl = new StringBuilder(GATEWAY_URL);
        if (sendMethod == FREE) {
            tmpUrl.append(FREE_SUB);
        } else if (sendMethod == LANDLINE) {
            tmpUrl.append(LANDLINE_SUB);
        } else {
            tmpUrl.append(SMS_SUB);
        }

        switch (sendMethod) {
            case FREE:
            case LANDLINE:
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
        if (sendMethod != FREE) {
            String sender;
            if (sendMethod == SPEED) { //nobody cares about, but have to be set here
                sender = "SMSoIP";
            } else {
                sender = findSenderAndWriteIfAvailable();
                if (sender.equals("")) {
                    provider.saveState();
                    return FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receivers);
                }
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
        if (returnInt == 100 || returnInt == 101) {           //write the last input as suggest for the sending
            provider.writeFreeInputSender();
        } else {
            provider.saveState();
        }
        return FireSMSResultList.getAllInOneResult(getErrorMessageByResult(returnInt), receivers);
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
        String sender = provider.getSenderFromFieldOrOptions();
        if (sender == null || sender.equals("")) {
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
                provider.writeDefaultSender(sender);
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
        int sendMethod = findSendMethod(spinnerText);
        return sendMethod != FREE && sendMethod != LANDLINE;
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
                return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_100));
            case 101:
                return SMSActionResult.NO_ERROR(provider.getTextByResourceId(R.string.return_101));
            case 111:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_111));
            case 112:
                SMSActionResult smsActionResult = SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_112));
                smsActionResult.setRetryMakesSense(false);
                return smsActionResult;
            case 120:
                provider.resetSender();
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_120));
            case 121:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_121));
            case 122:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_122));
            case 123:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_123));
            case 129:
                provider.resetSender();
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_129));
            case 130:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_130));
            case 134:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_134));
            case 140:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_140));
            case 150:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_150));
            case 161:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_161));
            case 162:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_162));
            case 170:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_170));
            case 171:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_171));
            case 172:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_172));
            default:
                return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.return_unknown) + " " + returnInt);
        }
    }


}
