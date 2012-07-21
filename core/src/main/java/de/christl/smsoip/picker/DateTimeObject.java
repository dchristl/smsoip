package de.christl.smsoip.picker;

import android.text.format.DateFormat;
import de.christl.smsoip.application.SMSoIPApplication;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Date and time object handling logic with ranges
 */
public class DateTimeObject {
    private Calendar calendar;

    private int lastMinute = -1;
    private int lastHour = -1;

    private final int minuteStepSize;
    private final SimpleDateFormat sdf = new SimpleDateFormat();

    public DateTimeObject(Calendar calendar, int minuteStepSize) {
        if (60 % minuteStepSize != 0) {
            throw new IllegalArgumentException("step size have to be a divisor of 60");
        }
        this.minuteStepSize = minuteStepSize;
        setMembers(calendar);
    }

    private void setMembers(Calendar calendar) {
        double currMin = calendar.get(Calendar.MINUTE);
        if (currMin % minuteStepSize != 0) {
            boolean toIncrease = lastMinute == -1;
            toIncrease |= lastMinute != 0 && currMin > lastMinute;
            toIncrease |= lastMinute == 0 && currMin - lastMinute < 30;
            if (toIncrease) { //round up
                currMin += ((double) minuteStepSize) / 2;

            } else { //round down
                currMin -= ((double) minuteStepSize) / 2;
            }
        }
        //now fix the number
        int newMin = (int) Math.round(currMin / minuteStepSize);
        newMin = newMin * minuteStepSize;
        calendar.set(Calendar.MINUTE, newMin);
        this.calendar = calendar;
        this.lastMinute = newMin;
    }

    public int getMinuteStepSize() {
        return minuteStepSize;
    }

    public int getHour() {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    public int getMinute() {
        return calendar.get(Calendar.MINUTE);
    }

    public void setTime(int hourOfDay, int minute) {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, hourOfDay);
        instance.set(Calendar.MINUTE, minute);
        setMembers(instance);
    }

    public Calendar getCalendar() {
        return calendar;
    }

    @Override
    public String toString() {
        return sdf.format(calendar.getTime());
    }

    public String timeString() {
        return DateFormat.getTimeFormat(SMSoIPApplication.getApp()).format(calendar.getTime());
    }

    public String dayString() {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(calendar.getTime());
    }
}
