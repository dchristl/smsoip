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

package de.christl.smsoip.picker.day;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import de.christl.smsoip.picker.DateTimeObject;

/**
 * Day picker with range handling
 */
public class RangeDayPickerDialog extends DatePickerDialog {

    private DateTimeObject dateTime;

    public RangeDayPickerDialog(Context context, OnDateSetListener callBack, DateTimeObject dateTime) {
        super(context, callBack, dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
        this.dateTime = dateTime.copy();
        setTitle(dateTime.toString());
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        dateTime.setDay(year, month, day);
        if (dateTime.getYear() != year || dateTime.getMonth() != month || dateTime.getDay() != day) {
            updateDate(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
        }
        setTitle(dateTime.toString());
    }

}
