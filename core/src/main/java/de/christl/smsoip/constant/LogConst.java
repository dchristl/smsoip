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

package de.christl.smsoip.constant;

/**
 * Class for logging facility
 * helps to increase the size strings are used
 */
public abstract class LogConst {
    private LogConst() {

    }

    //Bitmap Processor
    public static final String CALCULATE_IN_SAMPLE_SIZE = "calculateInSampleSize";
    public static final String DECODE_AND_SAVE_IMAGES = "decodeAndSaveImages";
    public static final String DECODE_IMAGE = "decodeImage";
    public static final String CALCULATE_RATIO = "calculateRatio";
    public static final String REMOVE_BACKGROUND_IMAGES = "removeBackgroundImages";
    public static final String GET_BACKGROUND_IMAGE = "getBackgroundImage";
    public static final String IS_BACKGROUND_IMAGE_SET = "isBackgroundImageSet";
    public static final String SAVE_IMAGE = "saveImage";

    //SendActivity
    public static final String ON_RESUME = "onResume";
    public static final String ON_CREATE = "onCreate";
    public static final String SET_DATE_TIME_PICKER_DIALOG = "setDateTimePickerDialog";
    public static final String SHOW_PROVIDERS_DIALOG = "showProvidersDialog";
    public static final String UPDATE_INFO_TEXT_SILENT = "updateInfoTextSilent";
    public static final String LAST_MESSAGES_BUTTON_CLICKED = "lastMessagesButtonClicked";
    public static final String SHOW_CHOSEN_CONTACTS_DIALOG = "showChosenContactsDialog";
    public static final String REFRESH_CLICKED = "Refresh clicked";
    public static final String KILL_DIALOG_AFTER_A_WHILE = "killDialogAfterAWhile";
    public static final String SEND_BY_THREAD = "sendByThread";
    public static final String REFRESH_INFORMATION_TEXT = "refreshInformationText ";
    public static final String ADD_TO_RECEIVER = "addToReceiver";
    public static final String ON_CREATE_DIALOG = "onCreateDialog ";
    public static final String CHANGE_SUPPLIER = "changeSupplier ";


    //GlobalPreference
    public static final String PROCESS_IMAGE_AND_SET_BACKGROUND_TASK_CREATED_AND_STARTED = "ProcessImageAndSetBackgroundTask created and started";

    //MultipleAccountPreference
    public static final String SHOW_USER_NAME_PASSWORD_DIALOG_CLOSED = "showUserNamePasswordDialog closed";
    public static final String SHOW_USER_NAME_PASSWORD_DIALOG = "showUserNamePasswordDialog";
    public static final String CHECK_CREDENTIALS_ON_CLICK = "checkCredentials onClick";
    public static final String REMOVE_ACCOUNT_ON_CLICK = "removeAccount onClick";
    public static final String EDIT_ACCOUNT_ONCLICK = "editAccount onclick";

    //Threading
    public static final String BACKGROUND_CHECK_LOGIN_TASK_STARTED = "BackgroundCheckLoginTask started";
    public static final String BACKGROUND_CHECK_LOGIN_TASK_RUNNING = "BackgroundCheckLoginTask running";
    public static final String BACKGROUND_CHECK_LOGIN_TASK_ON_FINISH = "BackgroundCheckLoginTask onFinish";
    public static final String BACKGROUND_UPDATE_STARTED = "background update started";
    public static final String BACKGROUND_UPDATE_ON_POST_EXECUTE = "background update on post execute";

    public static final String PROCESS_IMAGE_AND_SET_BACKGROUND_TASK_STARTED = "ProcessImageAndSetBackgroundTask started";
    public static final String PROCESS_IMAGE_AND_SET_BACKGROUND_TASK_POST_EXECUTE = "ProcessImageAndSetBackgroundTask postExecute";


    //Broadcast
    public static final String MESSAGE_RECEIVED_BY_RECEIVER = "message received by receiver";
}
