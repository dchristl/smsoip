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

package de.christl.smsoip.provider.versioned;

import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.picker.DateTimeObject;

import java.io.IOException;
import java.util.List;

/**
 * indicates if supplier can send time shift sms
 */
public interface TimeShiftSupplier {

    FireSMSResultList fireTimeShiftSMS(String smsText, List<Receiver> receivers, String spinnerText, DateTimeObject dateTime) throws IOException, NumberFormatException;

    int getMinuteStepSize();

    int getDaysInFuture();

    boolean isSendTypeTimeShiftCapable(String spinnerText);
}
