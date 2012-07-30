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

    private int minuteStepSize;
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
        return new SimpleDateFormat().format(calendar.getTime());
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
        toCompareInstance.set(Calendar.HOUR_OF_DAY, 0);   //ignore time
        toCompareInstance.set(Calendar.MINUTE, 0);
        toCompareInstance.set(Calendar.MILLISECOND, 0);
        if (toCompareInstance.before(instance)) {
            toCompareInstance.set(Calendar.DAY_OF_MONTH, toCompareInstance.get(Calendar.DAY_OF_MONTH) + daysInFuture);
            if (!toCompareInstance.after(instance)) {
                instance.set(Calendar.DAY_OF_MONTH, toCompareInstance.get(Calendar.DAY_OF_MONTH));
                instance.set(Calendar.YEAR, toCompareInstance.get(Calendar.YEAR));
                instance.set(Calendar.MONTH, toCompareInstance.get(Calendar.MONTH));
            }
            this.calendar = instance;
        }
    }


    public DateTimeObject copy() {
        return new DateTimeObject(calendar, minuteStepSize, daysInFuture);
    }

    public void setDaysInFuture(int daysInFuture) {
        this.daysInFuture = daysInFuture;
        setDayMembers(calendar);
    }

    public void setMinuteStepSize(int minuteStepSize) {
        this.minuteStepSize = minuteStepSize;
        setTimeMembers(calendar);
    }
}
