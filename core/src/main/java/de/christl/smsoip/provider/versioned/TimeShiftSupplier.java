package de.christl.smsoip.provider.versioned;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.picker.DateTimeObject;

import java.util.List;

/**
 * indicates if supplier can send time shift sms
 */
public interface TimeShiftSupplier {

    FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime);
}
