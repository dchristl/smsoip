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

    private final int minuteStepSize;
    private final SimpleDateFormat sdf = new SimpleDateFormat();
    private int daysInFuture;


    public DateTimeObject(Calendar calendar, int minuteStepSize, int daysInFuture) {
        this.daysInFuture = daysInFuture;
        if (60 % minuteStepSize != 0) {
            throw new IllegalArgumentException("step size have to be a divisor of 60");
        }
        this.minuteStepSize = minuteStepSize;
        setTimeMembers(calendar);
    }

    private void setTimeMembers(Calendar calendar) {
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
        instance.set(Calendar.YEAR, this.calendar.get(Calendar.YEAR));
        instance.set(Calendar.MONTH, this.calendar.get(Calendar.MONTH));
        instance.set(Calendar.DAY_OF_MONTH, this.calendar.get(Calendar.DAY_OF_MONTH));
        setTimeMembers(instance);
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


    public int getYear() {
        return calendar.get(Calendar.YEAR);
    }

    public int getMonth() {
        return calendar.get(Calendar.MONTH);
    }

    public int getDay() {
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public void setDay(int year, int month, int day) {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
        instance.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
        instance.set(Calendar.YEAR, year);
        instance.set(Calendar.MONTH, month);
        instance.set(Calendar.DAY_OF_MONTH, day);
        setDayMembers(instance);
    }

    private void setDayMembers(Calendar instance) {
        Calendar toCompareInstance = Calendar.getInstance();
        if (toCompareInstance.before(instance)) {
            toCompareInstance.set(Calendar.DAY_OF_MONTH, toCompareInstance.get(Calendar.DAY_OF_MONTH) + daysInFuture);
            if (toCompareInstance.after(instance)) {
                this.calendar = instance;
            }
        }
    }


    public DateTimeObject copy() {
        return new DateTimeObject(calendar, minuteStepSize, daysInFuture);
    }
}
