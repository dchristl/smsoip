package de.christl.smsoip.provider;

import android.text.Editable;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;

import java.util.List;

/**
 * Interface representing a supplier for sending sms
 */
public interface SMSSupplier {

    /**
     * HTTP Useragent.
     */
    static final String TARGET_AGENT = "Mozilla/3.0 (compatible)";
    /**
     * TIMEOUT can be used for any connection
     */
    static final int TIMEOUT = 10000;

    /**
     * this will called when the refresh button is used, is splitted by refreshInformationAfterMessageSuccessfulSent
     * for possibility to increase performance
     * (e.g. call login twice)
     *
     * @return Result.NO_ERRORS add additional info for the informations next to refresh button<br/>
     *         any other error if not successful
     */
    Result refreshInformationOnRefreshButtonPressed();

    /**
     * this will called after message is sent successful, is splitted by refreshInformationOnRefreshButtonPressed
     * for possibility to increase performance
     * (e.g. call login twice on message sent and on refresh)
     *
     * @return Result.NO_ERRORS add additional info for the informations next to refresh button<br/>
     *         any other error if not successful
     */
    Result refreshInformationAfterMessageSuccessfulSent();

    /**
     * the additional provider info, will be put before the sms message in database
     *
     * @return a short string for the provider
     */
    String getProviderInfo();

    /**
     * an array of all items for the optional spinner at sendactivity
     * if null or empty no spinner will be visible
     *
     * @return an array of items or null
     */
    String[] getSpinnerItems();

    /**
     * the corresponding provider of this supplier
     *
     * @return corresponding provider
     */
    OptionProvider getProvider();

    /**
     * will be called by checking the login in the option dialog
     *
     * @param userName - the username
     * @param password - the password
     * @return Result.NO_ERRORS -> Strings will be marked green, otherwise red
     */
    Result login(String userName, String password);

    /**
     * will be called on send SMS button
     *
     * @param smsText     - the message text
     * @param receivers
     * @param spinnerText - the text of the spinner or null if not visible  @return Result.NO_ERRORS on success or any other otherwise
     */
    Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText);
}
