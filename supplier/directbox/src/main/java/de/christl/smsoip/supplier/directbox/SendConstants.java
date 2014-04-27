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
    public static final String USER_NAME_FIELD_ID = "ctl00%24ContentPlaceHolder%24ctl00%24LoginPanel1%24tbIdentity=";
    public static final String PASSWORD_FIELD_ID = "ctl00%24ContentPlaceHolder%24ctl00%24LoginPanel1%24tbPassword=";
    public static final String LOGIN_BUTTON_PARAM = "ctl00%24ContentPlaceHolder%24ctl00%24LoginPanel1%24btnLogin=Anmelden";
    public static final String ASYNCPOST_PARAM = "__ASYNCPOST=true";
    public static final String EVENT_VALIDATION_LOGIN__PARAM = "__EVENTVALIDATION=";
    public static final String VIEWSTATE_LOGIN_PARAM = "__VIEWSTATE=";
    public static final String X_WWW_FORM_URL_ENCODING_WITH_UTF8 = "application/x-www-form-urlencoded";


    //BALANCE
    public static final String JSON_ENCODING = "application/json; charset=utf-8";
    public static final String JSON_BALANCE_CONTENT = "{\"request\":{\"__type\":\"Mediabeam.UMS.Dashboard.WidgetServiceRequest\",\"groupId\":0,\"dashboardId\":\"4444513\",\"widgetType\":\"2\",\"command\":\"load\",\"parameter\":{},\"objectInfo\":{\"uID\":\"1\",\"dID\":\"8601\",\"pID\":\"1\",\"objectID\":\"446365276\",\"widgetUID\":\"446365276\",\"objectType\":\"\",\"objHash\":\"Mediabeam.Web.UI.Dashboard.MixedDatabaseDashboardProvider\",\"groupId\":0,\"dashId\":\"4444513\"}}}";

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
