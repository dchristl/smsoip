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

package de.christl.smsoip.picker.time;

import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.TimePicker;
import de.christl.smsoip.picker.DateTimeObject;

/**
 * TimePicker with dateObject handling
 */
public class RangeTimePicker extends TimePickerDialog {

    private DateTimeObject dateTime;


    public RangeTimePicker(Context activity, OnTimeSetListener timePickerFragment, DateTimeObject dateTime, boolean hourFormat) {
        super(activity, timePickerFragment, dateTime.getHour(), dateTime.getMinute(), hourFormat);
        this.dateTime = dateTime.copy();
        setTitle(dateTime.toString());
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        dateTime.setTime(hourOfDay, minute);
        if (minute % dateTime.getMinuteStepSize() == 0) {
            super.onTimeChanged(view, hourOfDay, minute);
        } else {
            view.setCurrentMinute(dateTime.getMinute());
        }
        setTitle(dateTime.toString());
    }

}
