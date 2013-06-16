/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.constant;

/**
 * Class for Constants used by tracker
 */
public abstract class TrackerConstants {

    private TrackerConstants() {
    }

    public static final String CAT_SEND = "send";
    public static final String CAT_OPTIONS = "options";
    public static final String CAT_BUTTONS = "button";
    public static final String CAT_MISC = "miscellaneous";


    public static final String EVENT_NORMAL = "normal";
    public static final String EVENT_TIMESHIFT = "timeshift";
    public static final String EVENT_RATING = "rating";

    public static final String LABEL_MENU = "menu";
    public static final String LABEL_ICON = "icon";
    public static final String LABEL_POS = "positive";
    public static final String LABEL_NEG = "negative";
    public static final String LABEL_CANCEL = "cancel";



}
