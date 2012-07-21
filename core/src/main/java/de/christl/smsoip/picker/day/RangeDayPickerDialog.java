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
        this.dateTime = dateTime;
        setTitle(dateTime.toString());
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        dateTime.setDay(year, month, day);
        if (dateTime.getYear() == year && dateTime.getMonth() == month && dateTime.getDay() == day) {
            super.onDateChanged(view, year, month, day);
        } else {
            view.init(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), this);
        }
        setTitle(dateTime.toString());
    }
}
