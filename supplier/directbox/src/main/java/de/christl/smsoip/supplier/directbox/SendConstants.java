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

package de.christl.smsoip.supplier.directbox;


public abstract class SendConstants {

    //LOGIN
    private static final String EVENT_VALIDATION_LOGIN_CONTENT = "%2FwEWBQKd3oeiAgLU5py0AgLy0rfgCQLl9uWMDAL80euBDaUjHiMf7WB9Vxht2lY9PSxDMBYs";
    private static final String VIEW_STATE_LOGIN_CONTENT = "%2FwEPDwUJMjkxMDgzNjU0D2QWAmYPZBYEAgMPZBYCZg8PFgIeB1Zpc2libGVoZGQCBQ9kFgQCBQ8UKwACFCsAAw8WAh4XRW5hYmxlQWpheFNraW5SZW5kZXJpbmdoZGRkZGQCCQ9kFgICAQ9kFgICAQ8PFgIfAGhkZBgBBR5fX0NvbnRyb2xzUmVxdWlyZVBvc3RCYWNrS2V5X18WCgUXY3RsMDAkUmFkV2luZG93TWFuYWdlcjEFJWN0bDAwJE1lbnViYXJQbGFjZUhvbGRlciRSYWREb2NrWm9uZTQFIWN0bDAwJE1lbnViYXJQbGFjZUhvbGRlciRSYWREb2NrMQUaY3RsMDAkQ29udGVudFBsYWNlSG9sZGVyJEEFJGN0bDAwJENvbnRlbnRQbGFjZUhvbGRlciRMb2dpbldpZGdldAUaY3RsMDAkQ29udGVudFBsYWNlSG9sZGVyJDcFGmN0bDAwJENvbnRlbnRQbGFjZUhvbGRlciRCBRpjdGwwMCRDb250ZW50UGxhY2VIb2xkZXIkMwUaY3RsMDAkQ29udGVudFBsYWNlSG9sZGVyJDYFGmN0bDAwJENvbnRlbnRQbGFjZUhvbGRlciRDSlzhjHBK2GvYQvZjEIvaWEmNVYE%3D";

    public static final String USER_NAME_FIELD_ID = "ctl00%24ContentPlaceHolder%24LoginWidget%24C%24LoginPanel1%24tbIdentity=";
    public static final String PASSWORD_FIELD_ID = "ctl00%24ContentPlaceHolder%24LoginWidget%24C%24LoginPanel1%24tbPassword=";
    public static final String LOGIN_BUTTON_PARAM = "ctl00%24ContentPlaceHolder%24LoginWidget%24C%24LoginPanel1%24btnLogin=Anmelden";
    public static final String ASYNCPOST_PARAM = "__ASYNCPOST=true";
    public static final String EVENT_VALIDATION_LOGIN__PARAM = "__EVENTVALIDATION=" + EVENT_VALIDATION_LOGIN_CONTENT;
    public static final String VIEWSTATE_LOGIN_PARAM = "__VIEWSTATE=" + VIEW_STATE_LOGIN_CONTENT;
    public static final String X_WWW_FORM_URL_ENCODING_WITH_UTF8 = "application/x-www-form-urlencoded; charset=utf-8";


    //BALANCE
    public static final String JSON_ENCODING = "application/json; charset=utf-8";
    public static final String JSON_BALANCE_CONTENT = "{\"request\":{\"__type\":\"Mediabeam.UMS.Dashboard.WidgetServiceRequest\",\"groupId\":0,\"dashboardId\":\"4441463\",\"widgetType\":\"2\",\"command\":\"load\",\"parameter\":{},\"objectInfo\":{\"uID\":\"1\",\"dID\":\"8601\",\"pID\":\"1\",\"objectID\":\"446365276\",\"widgetUID\":\"446365276\",\"objectType\":\"\",\"objHash\":\"Mediabeam.Web.UI.Dashboard.MixedDatabaseDashboardProvider\",\"groupId\":0,\"dashId\":\"4441463\"}}}";

    //SENDING
    public static final String EVENT_VALIDATION_SEND__PARAM = "__EVENTVALIDATION=";
    public static final String VIEWSTATE_SEND_PARAM = "__VIEWSTATE=";
    public static final String SEND_BUTTON_PARAM = "ctl00%24ContentPlaceHolder%24btnSend2=Senden";
    public static final String TIME_FIELD_ID = "ctl00%24ContentPlaceHolder%24SendDateHiddenField=";
    public static final String FROM_FIELD_ID = "ctl00%24ContentPlaceHolder%24cbxFrom=";
    public static final String TO_FIELD_ID = "ctl00%24ContentPlaceHolder%24tbxTo=";
    public static final String TEXT_FIELD_ID = "ctl00%24ContentPlaceHolder%24TextEditor=";
    public static final String X_WWW_FORM_URL_ENCODING = "application/x-www-form-urlencoded";

}
