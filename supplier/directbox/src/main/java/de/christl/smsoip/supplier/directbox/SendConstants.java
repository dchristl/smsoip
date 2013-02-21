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
    private static final String EVENT_VALIDATION_LOGIN_CONTENT = "%2FwEWBQLu0IOvDQLZzqHBBALz0sbfBQLX%2BJLMDALwxqBLz1IsWYVpPWC%2B3xGtYFVi0pYOamQ%3D";
    private static final String VIEW_STATE_LOGIN_CONTENT = "%2FwEPDwUKMTQ5OTMzMTM0Nw9kFgJmD2QWBAIDD2QWAmYPDxYCHgdWaXNpYmxlaGRkAgUPZBYGAgUPFCsAAhQrAAMPFgIeF0VuYWJsZUFqYXhTa2luUmVuZGVyaW5naGRkZGRkAgkPZBYCAgEPZBYCAgEPDxYCHwBoZGQCDQ9kFgICAQ9kFgJmD2QWAgIBD2QWAgICD2QWAmYPZBYCAgEPZBYIAgEPDxYCHwBoZGQCBQ8PFgQeC05hdmlnYXRlVXJsBSEvcG9ydGFsL3JlZ2lzdHJhdGlvbi9yZWNvdmVyLmFzcHgeBFRleHQFE1Bhc3N3b3J0IHZlcmdlc3Nlbj9kZAIHDw8WBB8CBSIvcG9ydGFsL3JlZ2lzdHJhdGlvbi9yZWdpc3Rlci5hc3B4HwMFE0pldHp0IHJlZ2lzdHJpZXJlbiFkZAIJDw8WBB8DBQpTaWNoZXJoZWl0HwBoZGQYAQUeX19Db250cm9sc1JlcXVpcmVQb3N0QmFja0tleV9fFgEFF2N0bDAwJFJhZFdpbmRvd01hbmFnZXIxyq5P3ffIooqm16NJVmO3oPg2fnY%3D";

    public static final String USER_NAME_FIELD_ID = "ctl00%24ContentPlaceHolder%24ctl00%24LoginPanel1%24tbIdentity=";
    public static final String PASSWORD_FIELD_ID = "ctl00%24ContentPlaceHolder%24ctl00%24LoginPanel1%24tbPassword=";
    public static final String LOGIN_BUTTON_PARAM = "ctl00%24ContentPlaceHolder%24ctl00%24LoginPanel1%24btnLogin=Anmelden";
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
