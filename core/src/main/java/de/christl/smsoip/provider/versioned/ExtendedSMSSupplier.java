package de.christl.smsoip.provider.versioned;

import de.christl.smsoip.annotations.APIVersion;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.Map;

@APIVersion(minVersion = 14)
public interface ExtendedSMSSupplier extends SMSSupplier {

    /**
     * will be called on send SMS button
     *
     * @param smsText     - the message text
     * @param receivers   - indexed map of all receivers
     * @param spinnerText - the text of the spinner or null if not visible  @return Result.NO_ERRORS on ic_menu_success or any other otherwise
     */

    //TODO check if its better to use spinner item
    FireSMSResultList fireSMS(String smsText, Map<Integer, String> receivers, String spinnerText);
}
