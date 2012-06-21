package de.christl.smsoip.provider.versioned;

import de.christl.smsoip.annotations.APIVersion;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.List;

@APIVersion(minVersion = 14)
public interface ExtendedSMSSupplier extends SMSSupplier {

    /**
     * will be called on send SMS button
     *
     * @param smsText     - the message text
     * @param receivers   - lis of all receivers
     * @param spinnerText - the text of the spinner or null if not visible  @return Result.NO_ERRORS on ic_menu_success or any other otherwise
     */
    Result fireSMS(String smsText, List<String> receivers, String spinnerText);
}
