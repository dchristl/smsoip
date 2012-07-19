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
        this.dateTime = dateTime;
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
