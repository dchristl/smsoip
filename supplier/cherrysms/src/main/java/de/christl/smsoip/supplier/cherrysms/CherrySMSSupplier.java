package de.christl.smsoip.supplier.cherrysms;

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
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Supplier for Cherry-SMS
 */
public class CherrySMSSupplier implements ExtendedSMSSupplier {


    private static final String TARGET_URL = "https://gw.cherry-sms.com/?user=%s&password=%s";

    private CherrySMSOptionProvider provider;

    public CherrySMSSupplier() {
        provider = new CherrySMSOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) {
        if (userName == null || password == null) {
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }
        String tmpUrl;
        try {
            tmpUrl = getURLStringWithUserNameAndPassword(userName, password);
        } catch (UnsupportedEncodingException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        } catch (NoSuchAlgorithmException e) {
            return SMSActionResult.UNKNOWN_ERROR();
        }

        UrlConnectionFactory factory = new UrlConnectionFactory(tmpUrl, UrlConnectionFactory.METHOD_GET);
        try {
            HttpURLConnection httpURLConnection = factory.create();
            InputStream inputStream = httpURLConnection.getInputStream();

            String returnValue = UrlConnectionFactory.inputStream2DebugString(inputStream);
            int returnInt = Integer.parseInt(returnValue);
            if (returnInt == 10) { //expect wrong receiver number, is better than check for credits
                return SMSActionResult.LOGIN_SUCCESSFUL();
            }
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.NETWORK_ERROR();
        } catch (NumberFormatException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.LOGIN_FAILED_ERROR();
        }

        return SMSActionResult.UNKNOWN_ERROR(provider.getTextByResourceId(R.string.text_login_failed));
    }

    private String getURLStringWithUserNameAndPassword(String userName, String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        String md5Password = CherrySMSOptionProvider.getMD5String(password);
        return String.format(TARGET_URL, userName, md5Password);
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) {
        return null;
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() {
        return refreshInformations(true);
    }

    private SMSActionResult refreshInformations(boolean afterMessageSentSuccessful) {
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
            String returnValue = UrlConnectionFactory.inputStream2DebugString(httpURLConnection.getInputStream());
            int returnInt = Integer.parseInt(returnValue);
            //only valid value here will be 50 (login failed, but will not reached here if creentials errous)
            return SMSActionResult.NO_ERROR(String.format(tmpText, returnInt));
        } catch (UnsupportedEncodingException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR();
        } catch (NoSuchAlgorithmException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.UNKNOWN_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.NETWORK_ERROR();
        }

    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() {
        return refreshInformations(false);
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
