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

package de.christl.smsoip.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;

import org.acra.ACRA;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.ads.AdLayout;
import de.christl.smsoip.activities.dialogadapter.ChangeProviderArrayAdapter;
import de.christl.smsoip.activities.send.Mode;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.activities.threading.BackgroundSendTask;
import de.christl.smsoip.activities.threading.BackgroundUpdateTask;
import de.christl.smsoip.activities.threading.ThreadingUtil;
import de.christl.smsoip.application.AppRating;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.autosuggest.NameNumberSuggestField;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.database.AndroidInternalDatabaseHandler;
import de.christl.smsoip.database.Contact;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.patcher.InputPatcher;
import de.christl.smsoip.picker.DateTimeObject;
import de.christl.smsoip.picker.day.RangeDayPickerDialog;
import de.christl.smsoip.picker.time.RangeTimePicker;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import de.christl.smsoip.ui.CheckForDuplicatesArrayList;
import de.christl.smsoip.ui.ChosenContactsDialog;
import de.christl.smsoip.ui.EmoImageDialog;
import de.christl.smsoip.ui.SMSInputEditText;
import de.christl.smsoip.ui.SendMessageDialog;
import de.christl.smsoip.ui.ShowLastMessagesDialog;
import de.christl.smsoip.ui.SmileyDialog;

import static de.christl.smsoip.constant.TrackerConstants.CAT_BUTTONS;
import static de.christl.smsoip.constant.TrackerConstants.CAT_OPTIONS;
import static de.christl.smsoip.constant.TrackerConstants.CAT_SEND;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_CLEAR;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_CONVERSATION;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_LAST_INFO;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_MODE_TOGGLE;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_NORMAL;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_SHORTEN;
import static de.christl.smsoip.constant.TrackerConstants.EVENT_TIMESHIFT;
import static de.christl.smsoip.constant.TrackerConstants.LABEL_ICON;
import static de.christl.smsoip.constant.TrackerConstants.LABEL_MENU;


public class SendActivity extends AllActivity {


    private NameNumberSuggestField receiverField;
    private SMSInputEditText textField;
    private TextView smssigns;
    private Spinner spinner;

    private static final int PICK_CONTACT_REQUEST = 0;

    private Mode mode = Mode.NORMAL;
    private SMSoIPPlugin smSoIPPlugin;


    private static final int PROVIDER_OPTION = 30;
    private static final int OPTION_SWITCH_SUPPLIER = 31;
    public static final int DIALOG_SMILEYS = 32;
    private static final int DIALOG_PROVIDER = 33;
    private static final int GLOBAL_OPTION = 34;
    private static final int OPTION_SWITCH_ACCOUNT = 35;
    private static final int DIALOG_SWITCH_ACCOUNT = 36;
    private static final int DIALOG_TEXT_MODULES = 37;

    private ChosenContactsDialog chosenContactsDialog;

    private static final String SAVED_INSTANCE_SUPPLIER = "supplier";
    private static final String SAVED_INSTANCE_INPUTFIELD = "inputfield";
    private static final String SAVED_INSTANCE_RECEIVERS = "receivers";
    private static final String SAVED_INSTANCE_SPINNER = "spinner";
    private static final String SAVED_INSTANCE_INFO = "info";
    private static final String SAVED_INSTANCE_MODE = "mode";
    private static final String SAVED_INSTANCE_ACCOUNT_ID = "account";
    private static final String SAVED_INSTANCE_DATE_TIME = "datetime";
    private static final String SAVED_INSTANCE_LAST_INFO_DIALOG_CONTENT = "lastInfoDialogContent";
    private static final String SAVED_INSTANCE_LAST_INFO_DIALOG_RESULT = "result";

    private boolean optionsCalled = false;
    private boolean providerOptionsCalled = false;

    private String lastInfoDialogContent;
    private FireSMSResultList.SendResult result;

    private DateTimeObject dateTime;
    private AsyncTask<Boolean, SMSActionResult, SMSActionResult> backgroundUpdateTask;
    private Integer currentAccountIndex;

    private ColorStateList defaultColor;
    private BackgroundSendTask backgroundSendTask;


    @Override
    /**
     * entry point on resuming back to main activity
     */
    protected void onResume() {
        super.onResume();
        ErrorReporterStack.put(LogConst.ON_RESUME);
        //this is for performance cases and can cause issues in some case:
        // if options are called a refresh will be forced because settings can change
        // if activity is killed a new instance will be creeated automatically (and options are "fresh")
        // refresh will only be called if kill not happens
        // otherwise saved instance states are overwritten
        if (smSoIPPlugin != null && optionsCalled) {
            smSoIPPlugin.getProvider().refresh();
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            if (currentAccountIndex != null) { //set back the current account
                smSoIPPlugin.getProvider().setCurrentAccountId(currentAccountIndex);
            }
            setFullTitle();
            invalidateOptionsMenu();//if user disables/enables button
            float fontSize = settings.getFloat(SettingsConst.GLOBAL_FONT_SIZE_FACTOR, 1.0f) * 15;
            ((TextView) findViewById(R.id.textInput)).setTextSize(fontSize);
            textField.refreshTextModules();
            optionsCalled = false;
            if (infoTextUpdateNeeded()) {
                refreshInformationText(true);
            }
        }
        if (providerOptionsCalled) {
            updateInfoTextSilent();
            invalidateOptionsMenu();
            providerOptionsCalled = false;
        }
    }

    /**
     * check if auto aupdate is enabled and text is on default
     *
     * @return
     */
    private boolean infoTextUpdateNeeded() {
        boolean settingActivated = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsConst.GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP, false);
        String string = getString(R.string.notyetrefreshed);
        TextView infoText = (TextView) findViewById(R.id.infoText);
        boolean defaultText = infoText.getText().equals(string);
        return settingActivated && defaultText;
    }


    @Override
    /**
     * main entry point for new activity
     */
    public void onCreate(Bundle savedInstanceState) {
        SMSoIPApplication app = SMSoIPApplication.getApp();
        app.initProviders();
        super.onCreate(savedInstanceState);
        app.throwlastExceptionIfAny();
//        SMSReceiver.faker(this);
        //save the default color of textview
        new AppRating(this).showRateDialogIfNeeded();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.sendactivity);
        setAutoSuggestField();
        smssigns = (TextView) findViewById(R.id.smssigns);
        smssigns.setText(String.format(getText(R.string.smssigns).toString(), 0, 0));
        mode = settings.getBoolean(SettingsConst.GLOBAL_ENABLE_COMPACT_MODE, false) ? Mode.COMPACT : Mode.NORMAL;
        //disable inputs on field
        setSearchButton();
        setCustomActionBar();
        setClearButton();
        setRefreshButton();
        setTextModulesButton();
        setShowChosenContactsDialog();
        setShortTextButton();
        setSmileyButton();
        setTextArea();
        setSendButton();
        setLastInfoButton();
        setLastMessagesButton();
        addModeSwitcher();
        boolean isResumed = savedInstanceState != null && savedInstanceState.getString(SAVED_INSTANCE_SUPPLIER) != null && !savedInstanceState.getString(SAVED_INSTANCE_SUPPLIER).equals("") && app.getSMSoIPPluginBySupplierName(savedInstanceState.getString(SAVED_INSTANCE_SUPPLIER)) != null;
        if (isResumed) {  //activity was killed and is resumed
            smSoIPPlugin = app.getSMSoIPPluginBySupplierName(savedInstanceState.getString(SAVED_INSTANCE_SUPPLIER));
            smSoIPPlugin.getProvider().afterActivityKilledAndOnCreateCalled(savedInstanceState);
            setFullTitle();
            setSpinner();
            long timeInMillis = savedInstanceState.getLong(SAVED_INSTANCE_DATE_TIME, -1);
            if (timeInMillis != -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(timeInMillis);
                TimeShiftSupplier timeShiftSupplier = smSoIPPlugin.getTimeShiftSupplier();
                dateTime = new DateTimeObject(calendar, timeShiftSupplier.getMinuteStepSize(), timeShiftSupplier.getDaysInFuture());
            }
            if (spinner.getVisibility() == View.VISIBLE) { //if the spinner is visible, the  spinner item is selected, too
                spinner.setSelection(savedInstanceState.getInt(SAVED_INSTANCE_SPINNER, 0), false);
            }
            mode = Mode.values()[savedInstanceState.getInt(SAVED_INSTANCE_MODE)];
            setDateTimePickerDialog();
            receiverField.setText(savedInstanceState.getCharSequence(SAVED_INSTANCE_INPUTFIELD));
            ArrayList<Receiver> tmpReceiverList = savedInstanceState.getParcelableArrayList(SAVED_INSTANCE_RECEIVERS);
            CheckForDuplicatesArrayList receiverList = new CheckForDuplicatesArrayList(); //simple copy, cause of unknown compile error
            receiverList.addAll(tmpReceiverList);
            receiverField.setReceiverList(receiverList);
            int accountIndex = savedInstanceState.getInt(SAVED_INSTANCE_ACCOUNT_ID);
            switchAccount(accountIndex);
            updateInfoText(savedInstanceState.getString(SAVED_INSTANCE_INFO));
            lastInfoDialogContent = savedInstanceState.getString(SAVED_INSTANCE_LAST_INFO_DIALOG_CONTENT);
            String enumS = savedInstanceState.getString(SAVED_INSTANCE_LAST_INFO_DIALOG_RESULT);
            if (enumS != null) {
                result = FireSMSResultList.SendResult.valueOf(enumS);
            }

            updateViewOnChangedReceivers(); //call it if a a receiver is appended
            setSuppliersLayout();
        } else {     // fresh create call on activity so do the default behaviour
            IntentHandler handler = new IntentHandler(getIntent(), this);
            getAndSetSupplier(handler);
            setPreselectedContact(handler);
            updateInfoTextSilent();
        }
        showChangelogIfNeeded();
        setViewByMode(mode);
        ErrorReporterStack.put(LogConst.ON_CREATE);
    }

    /**
     * wraperrr for the sherlock actionbar
     */
    private void setCustomActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        supportActionBar.setCustomView(R.layout.actionbar);
    }

    /**
     * set and enable the suggest field for the phone numbers
     */
    private void setAutoSuggestField() {

        receiverField = (NameNumberSuggestField) findViewById(R.id.receiverField);
        receiverField.addReceiverChangedListener(new NameNumberSuggestField.ReceiverChangedListener() {
            @Override
            public void onReceiverChanged(boolean addedTwice, boolean tooMuchReceivers) {
                setVisibilityByCurrentReceivers();
                if (tooMuchReceivers) {
                    showTooMuchReceiversToast();
                } else if (addedTwice) {
                    showAddedTwiceToast();
                }
            }
        });

    }

    /**
     * entry point for mode switching
     */
    private void addModeSwitcher() {
        View toggleUp = findViewById(R.id.viewToggleUp);
        View toggleDown = findViewById(R.id.viewToggleDown);
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> build = MapBuilder.createEvent(CAT_BUTTONS, EVENT_MODE_TOGGLE, mode.name(), null).build();
                EasyTracker.getInstance(SendActivity.this).send(build);
                switch (mode) {
                    case NORMAL:
                        mode = Mode.COMPACT;
                        setViewByMode(mode);
                        break;
                    case COMPACT:
                        mode = Mode.NORMAL;
                        setViewByMode(mode);
                        break;
                }
            }
        };
        toggleDown.setOnClickListener(l);
        toggleUp.setOnClickListener(l);
    }


    /**
     * set the visibility of all stuff in GUI mode depnedent
     *
     * @param mode
     */
    private void setViewByMode(Mode mode) {
        View but1 = findViewById(R.id.tblButton1);
        View but2 = findViewById(R.id.tblButton2);
        View tsRow = findViewById(R.id.tblTimeShiftRow);
        View tsDescr = findViewById(R.id.tblSendingTimeDescr);
        View stRow = findViewById(R.id.tblSendingTypeSpinner);
        View stDescr = findViewById(R.id.tblSendingTypeDescr);
        View infoTextUpper = findViewById(R.id.infoTextUpper);
        View freeLayout = findViewById(R.id.tblFreeLayout);
        View progressUpper = findViewById(R.id.infoTextProgressBarUpper);
        View progress = findViewById(R.id.infoTextProgressBar);
        View toggleUp = findViewById(R.id.viewToggleUp);
        View toggleDown = findViewById(R.id.viewToggleDown);
        View adTop = findViewById(R.id.adLayoutUpper);
        View adBottom = findViewById(R.id.adLayoutLower);
        switch (mode) {
            case NORMAL:
                but1.setVisibility(View.VISIBLE);
                but2.setVisibility(View.VISIBLE);
                tsRow.setVisibility(View.VISIBLE);
                tsDescr.setVisibility(View.VISIBLE);
                stRow.setVisibility(View.VISIBLE);
                stDescr.setVisibility(View.VISIBLE);
                freeLayout.setVisibility(View.VISIBLE);
                infoTextUpper.setVisibility(View.GONE);
                progressUpper.setVisibility(View.GONE);
                toggleDown.setVisibility(View.VISIBLE);
                toggleUp.setVisibility(View.INVISIBLE);
                adTop.setVisibility(View.VISIBLE);
                adBottom.setVisibility(View.GONE);

                break;
            case COMPACT:
                infoTextUpper.setVisibility(View.VISIBLE);
                but1.setVisibility(View.GONE);
                but2.setVisibility(View.GONE);
                tsRow.setVisibility(View.GONE);
                tsDescr.setVisibility(View.GONE);
                stRow.setVisibility(View.GONE);
                stDescr.setVisibility(View.GONE);
                freeLayout.setVisibility(View.GONE);
                progressUpper.setVisibility(progress.getVisibility());
                toggleDown.setVisibility(View.INVISIBLE);
                toggleUp.setVisibility(View.VISIBLE);
                adTop.setVisibility(View.GONE);
                adBottom.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * set the dialog for picking
     */
    private void setDateTimePickerDialog() {
        ErrorReporterStack.put(LogConst.SET_DATE_TIME_PICKER_DIALOG);
        View timeShiftLayout = findViewById(R.id.timeShiftLayout);
        View timeShiftDescr = findViewById(R.id.timeShiftDescr);
        final TextView timeText = (TextView) findViewById(R.id.timeText);
        final Button pickDay = (Button) findViewById(R.id.pickDay);
        final Button pickHour = (Button) findViewById(R.id.pickHour);
        String spinnerText = spinner.getVisibility() == View.INVISIBLE || spinner.getVisibility() == View.GONE ? null : spinner.getSelectedItem().toString();
        if (smSoIPPlugin.isTimeShiftCapable(spinnerText)) {
            timeShiftLayout.setVisibility(View.VISIBLE);
            timeShiftDescr.setVisibility(View.VISIBLE);
            if (dateTime != null) {
                TimeShiftSupplier timeShiftSupplier = smSoIPPlugin.getTimeShiftSupplier();
                dateTime.setMinuteStepSize(timeShiftSupplier.getMinuteStepSize());
                dateTime.setDaysInFuture(timeShiftSupplier.getDaysInFuture());
                pickDay.setVisibility(View.VISIBLE);
                pickHour.setVisibility(View.VISIBLE);
                pickDay.setText(dateTime.dayString());
                pickHour.setText(dateTime.timeString());
            }
            final TimePickerDialog.OnTimeSetListener timeListener = new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    if (dateTime != null) {
                        dateTime.setTime(hourOfDay, minute);
                        pickHour.setText(dateTime.timeString());
                    }
                }
            };
            final DatePickerDialog.OnDateSetListener dayListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    if (dateTime != null) {
                        dateTime.setDay(year, monthOfYear, dayOfMonth);
                        pickDay.setText(dateTime.dayString());
                    }
                }
            };
            final CheckBox pickTimeCheckBox = (CheckBox) findViewById(R.id.pickTime);
            View.OnClickListener switchCheckBoxListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pickTimeCheckBox.setChecked(true);
                }
            };
            timeText.setOnClickListener(switchCheckBoxListener);
            timeShiftLayout.setOnClickListener(switchCheckBoxListener);
            final View.OnClickListener pickHourListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RangeTimePicker rangeTimePicker = new RangeTimePicker(SendActivity.this, timeListener, dateTime, DateFormat.is24HourFormat(SendActivity.this));
                    rangeTimePicker.show();
                }
            };
            final View.OnClickListener pickDayListener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    RangeDayPickerDialog dayPickerDialog = new RangeDayPickerDialog(SendActivity.this, dayListener, dateTime);
                    dayPickerDialog.show();
                }
            };

            pickTimeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (dateTime == null) {
                            TimeShiftSupplier timeShiftSupplier = smSoIPPlugin.getTimeShiftSupplier();
                            Calendar instance = Calendar.getInstance();
                            //add 2 minutes to be in future instead of now
                            instance.add(Calendar.MINUTE, 2);
                            dateTime = new DateTimeObject(instance, timeShiftSupplier.getMinuteStepSize(), timeShiftSupplier.getDaysInFuture());
                        }
                        pickDay.setText(dateTime.dayString());
                        pickHour.setText(dateTime.timeString());
                        pickHour.setOnClickListener(pickHourListener);
                        pickDay.setOnClickListener(pickDayListener);
                        timeText.setVisibility(View.GONE);
                        pickHour.setVisibility(View.VISIBLE);
                        pickDay.setVisibility(View.VISIBLE);
                    } else {
                        pickHour.setOnClickListener(null);
                        pickDay.setOnClickListener(null);
                        timeText.setText(R.string.now);
                        timeText.setVisibility(View.VISIBLE);
                        pickHour.setVisibility(View.GONE);
                        pickDay.setVisibility(View.GONE);
                        dateTime = null;
                    }

                }
            });

        } else {
            timeShiftLayout.setVisibility(View.GONE);
            timeShiftDescr.setVisibility(View.GONE);
            timeText.setOnClickListener(null);
            timeShiftLayout.setOnClickListener(null);
            timeText.setText(R.string.now);
            pickHour.setVisibility(View.GONE);
            pickDay.setVisibility(View.GONE);
        }
    }

    /**
     * show the dialog for all providers dependent on the count of it
     */
    private void showProvidersDialog() {
        ErrorReporterStack.put(LogConst.SHOW_PROVIDERS_DIALOG);
        Map<String, SMSoIPPlugin> providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        final List<SMSoIPPlugin> filteredProviderEntries = new ArrayList<SMSoIPPlugin>();
        if (smSoIPPlugin == null) {   //add all if current provider not set
            filteredProviderEntries.addAll(providerEntries.values());
        } else {
            for (SMSoIPPlugin providerEntry : providerEntries.values()) {     //filter out cause current provider should not be shown
                if (!providerEntry.getSupplierClassName().equals(smSoIPPlugin.getSupplierClassName())) {
                    filteredProviderEntries.add(providerEntry);
                }
            }
        }
        if (filteredProviderEntries.size() == 1) {
            changeSupplier(filteredProviderEntries.get(0).getSupplierClassName());
        }
        //only show the dialog if more thann two providers are installed otherwise NPE on some devices
        else if (filteredProviderEntries.size() > 1) { //skip if no provider available
            removeDialog(DIALOG_PROVIDER);
            showDialog(DIALOG_PROVIDER);
        }
    }

    /**
     * update the info text in background
     */
    private void updateInfoTextSilent() {
        ErrorReporterStack.put(LogConst.UPDATE_INFO_TEXT_SILENT);
        //only if parameter and supplier set
        final TextView infoText = (TextView) findViewById(R.id.infoText);
        final TextView infoTextUpper = (TextView) findViewById(R.id.infoTextUpper);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsConst.GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP, false) && smSoIPPlugin != null) {
            infoText.setText(R.string.notyetrefreshed);
            infoTextUpper.setText(getString(R.string.notyetrefreshed) + " " + getString(R.string.tap));
            refreshInformationText(true);

        } else {
            infoText.setText(R.string.notyetrefreshed);
            infoTextUpper.setText(getString(R.string.notyetrefreshed) + " " + getString(R.string.tap));
        }
    }

    /**
     * enables the progress bars on top of the sms text and next to refresh button, after refresh button is pressed
     */
    public synchronized void showUpdateProgressBar() {
        TextView infoText = (TextView) findViewById(R.id.infoText);
        TextView infoTextUpper = (TextView) findViewById(R.id.infoTextUpper);
        infoText.setText("");
        infoTextUpper.setText("");
        ProgressBar progressUpper = (ProgressBar) findViewById(R.id.infoTextProgressBarUpper);
        ProgressBar progress = (ProgressBar) findViewById(R.id.infoTextProgressBar);
        if (mode.equals(Mode.COMPACT)) {    //only set the progress bar visible in comnpact mode
            progressUpper.setVisibility(View.VISIBLE);
        }
        progress.setVisibility(View.VISIBLE);
    }

    /**
     * updates the info texts by given text
     * also sets the progress bars back to invisible
     *
     * @param info
     */
    public synchronized void updateInfoText(String info) {
        TextView infoText = (TextView) findViewById(R.id.infoText);
        TextView infoTextUpper = (TextView) findViewById(R.id.infoTextUpper);
        ProgressBar progressUpper = (ProgressBar) findViewById(R.id.infoTextProgressBarUpper);
        ProgressBar progress = (ProgressBar) findViewById(R.id.infoTextProgressBar);
        progress.setVisibility(View.INVISIBLE);
        progressUpper.setVisibility(View.INVISIBLE);
        if (info != null) {
            infoText.setText(info);
            infoTextUpper.setText(info + " " + getString(R.string.tap));
        }
    }

    /**
     * use it for setting the info text back to default (not yet refreshed )
     */
    public void resetInfoText() {
        updateInfoText(getString(R.string.notyetrefreshed));
    }

    /**
     * set the info button for reshowing the last send result
     */
    private void setLastInfoButton() {
        View showInfoButton = findViewById(R.id.showInfoButton);
        showInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> build = MapBuilder.createEvent(CAT_BUTTONS, EVENT_LAST_INFO, "", null).build();
                EasyTracker.getInstance(SendActivity.this).send(build);
                if (lastInfoDialogContent != null && result != null) {
                    EmoImageDialog lastInfoDialog = new EmoImageDialog(SendActivity.this, result, lastInfoDialogContent);
                    lastInfoDialog.show();
                    lastInfoDialog.setCancelable(true);
                }
            }
        });
    }

    /**
     * build and set the title of the activity
     */
    private void setFullTitle() {
        final OptionProvider provider = smSoIPPlugin.getProvider();
        String userName = provider.getUserName() == null ? getString(R.string.account_no_account) : provider.getUserName();

        TextView actionBarText = (TextView) findViewById(R.id.actionBarText);
        actionBarText.setSelected(true);
        String providerTitleString = " (" + provider.getProviderName() + ")";
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(userName);
        spannableStringBuilder.setSpan(new ForegroundColorSpan(Color.WHITE), 0, userName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!userName.toLowerCase().contains(provider.getProviderName().toLowerCase())) {
            spannableStringBuilder.append(providerTitleString);
            spannableStringBuilder.setSpan(new ForegroundColorSpan(Color.GRAY), userName.length() + 1, spannableStringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        actionBarText.setText(spannableStringBuilder);
        setSuppliersLayout();
        actionBarText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OptionProvider provider = smSoIPPlugin.getProvider();
                Map<Integer, AccountModel> accounts = provider.getAccounts();
                Map<String, String> build;
                if (accounts.size() > 0) {
                    build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(OPTION_SWITCH_ACCOUNT), LABEL_ICON, null).build();
                    showAccountDialog();
                } else {
                    build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(PROVIDER_OPTION), LABEL_ICON, null).build();
                    startOptionActivity();
                }
                EasyTracker.getInstance(SendActivity.this).send(build);
            }
        });
        Drawable iconDrawable = smSoIPPlugin.getProvider().getIconDrawable();
        View viewById = findViewById(R.id.actionBarLogo);
        if (iconDrawable != null) {
            viewById.setBackgroundDrawable(iconDrawable);
        } else {
            viewById.setBackgroundResource(R.drawable.icon);
        }
        viewById.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(OPTION_SWITCH_SUPPLIER), LABEL_ICON, null).build();
                EasyTracker.getInstance(SendActivity.this).send(build);
                showProvidersDialog();
            }
        });
    }

    /**
     * set the contact if this is forced "outside", like share functionality or notification
     *
     * @param handler
     */
    private void setPreselectedContact(IntentHandler handler) {
        SMSInputEditText smsInputEditText = (SMSInputEditText) findViewById(R.id.textInput);
        Receiver givenReceiver = handler.getGivenReceiver();
        boolean numberSet = givenReceiver != null;
        if (numberSet) {
            addReceiver(givenReceiver);
        }
        String smsBody = handler.getSmsText();
        boolean smsBodySet = smsBody != null && !smsBody.equals("");
        if (smsBodySet) {
            smsInputEditText.append(smsBody);
        }
        if (numberSet) { //when number given, set focus to the sms input field
            smsInputEditText.requestFocus();
            smsInputEditText.processReplacement();
        }
    }

    /**
     * build the send button
     */
    private void setSendButton() {

        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
                                          public void onClick(View view) {
                                              if (!preSendCheck()) {
                                                  return;
                                              }
                                              cancelUpdateTask();

                                              Dialog progressDialog = new SendMessageDialog(SendActivity.this);
                                              progressDialog.show();
                                              if (backgroundSendTask != null) {//should not happen because unbreakable
                                                  backgroundSendTask.cancel(true);
                                              }
                                              backgroundSendTask = new BackgroundSendTask(SendActivity.this, progressDialog);
                                              backgroundSendTask.execute();
                                          }
                                      }

        );
    }

    /**
     * build and get all informations needed for the dialog of last messages
     */
    private void setLastMessagesButton() {
        View showHistoryButton = findViewById(R.id.showHistory);
        boolean visible = SMSoIPApplication.getApp().isReadFromDatabaseAvailable() || SMSoIPApplication.getApp().isUseOwnDatabase();
        showHistoryButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        showHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, String> build = MapBuilder.createEvent(CAT_BUTTONS, EVENT_CONVERSATION, "", null).build();
                EasyTracker.getInstance(SendActivity.this).send(build);
                ErrorReporterStack.put(LogConst.LAST_MESSAGES_BUTTON_CLICKED);
                final ShowLastMessagesDialog lastMessageDialog = new ShowLastMessagesDialog(SendActivity.this, receiverField.getReceiverList());
                lastMessageDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        String receiverNumber = lastMessageDialog.getReceiverNumber();
                        if (receiverNumber != null) {
                            Receiver contactByNumber = AndroidInternalDatabaseHandler.findContactByNumber(receiverNumber, SendActivity.this);
                            if (contactByNumber == null) {
                                contactByNumber = new Receiver(getString(R.string.unknown));
                                contactByNumber.setRawNumber(receiverNumber, getString(R.string.no_phone_type_label));
                            }
                            addReceiver(contactByNumber);
                        }
                    }
                });
                lastMessageDialog.show();
            }
        });

    }

    /**
     * use it for getting and setting the supplier from "outside" (provider call)
     *
     * @param handler
     */
    private void getAndSetSupplier(IntentHandler handler) {
        String supplier = "";
        //only change if own scheme is used and
        String givenSupplier = handler.getSupplier();
        if (givenSupplier != null && receiverField.getReceiverList().size() == 0) {
            supplier = givenSupplier;
        }
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (supplier.equals("")) {
            supplier = settings.getString(SettingsConst.GLOBAL_DEFAULT_PROVIDER, "");
        }
        //check if default provider is installed
        if (!supplier.equals("")) {
            boolean found = false;
            for (SMSoIPPlugin providerEntry : SMSoIPApplication.getApp().getProviderEntries().values()) {
                if (providerEntry.getSupplierClassName().equals(supplier)) {
                    found = true;
                    break;
                }
            }
            if (!found) { //set back to default (always ask) if none found
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(SettingsConst.GLOBAL_DEFAULT_PROVIDER, null);
                editor.commit();
                supplier = null;
            }
        } else {
            supplier = null;
        }
        if (supplier == null) {
            showProvidersDialog();
        } else {
            changeSupplier(supplier);
        }
    }

    /**
     * set the dialog of all receivers if any
     */
    private void setShowChosenContactsDialog() {
        ImageButton chosenContactsdialogButton = (ImageButton) findViewById(R.id.showChosenContacts);
        chosenContactsdialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChosenContactsDialog();
            }
        });
    }

    /**
     * show the dialog of all receivers if any
     */
    private void showChosenContactsDialog() {
        ErrorReporterStack.put(LogConst.SHOW_CHOSEN_CONTACTS_DIALOG);
        chosenContactsDialog = new ChosenContactsDialog(this, receiverField.getReceiverList());
        chosenContactsDialog.setOwnerActivity(this);
        chosenContactsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateViewOnChangedReceivers();
            }
        });
        chosenContactsDialog.show();
    }

    @Override
    /**
     * handle for own if config has changed (like changing form landscape to portrait)
     */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        invalidateOptionsMenu();
        if (chosenContactsDialog != null && chosenContactsDialog.isShowing()) {
            chosenContactsDialog.redraw();
        }
    }

    /**
     * set the button for the smileys
     */
    private void setSmileyButton() {
        ImageButton smileyButton = (ImageButton) findViewById(R.id.insertSmileyButton);
        smileyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(DIALOG_SMILEYS);
            }
        });
    }

    /**
     * set the button for shortening text
     */
    private void setShortTextButton() {
        ImageButton shortTextButton = (ImageButton) findViewById(R.id.shortTextButton);
        shortTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, String> build = MapBuilder.createEvent(CAT_BUTTONS, EVENT_SHORTEN, "", null).build();
                EasyTracker.getInstance(SendActivity.this).send(build);
                String oldText = textField.getText().toString();
                if (!oldText.equals("")) {
                    Pattern p = Pattern.compile("\\s[a-zA-Z]");
                    Matcher m = p.matcher(oldText);
                    StringBuffer sb = new StringBuffer();
                    boolean result = m.find();
                    // Loop through and create a new String
                    // with the replacements
                    while (result) {
                        String hit = oldText.substring(m.start(), m.end()).trim().toUpperCase();
                        m.appendReplacement(sb, hit);
                        result = m.find();
                    }
                    // Add the last segment of input to
                    // the new String
                    m.appendTail(sb);

                    textField.setText(sb.toString().replaceAll("\\s", ""));
                } else {
                    Toast toast = Toast.makeText(SendActivity.this, R.string.nothing_to_shorten, Toast.LENGTH_LONG);
                    toast.show();
                }

            }
        });
    }

    /**
     * set the button for the text modules
     */
    private void setTextModulesButton() {
        ImageButton sigButton = (ImageButton) findViewById(R.id.insertSigButton);
        sigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTextModulesDialog();

            }
        });
    }

    /**
     * show the dialog for text modules (count dependent)
     */
    public void showTextModulesDialog() {
        Map<String, String> textModules = textField.getTextModules();
        if (textModules.size() > 1) {
            removeDialog(DIALOG_TEXT_MODULES);//force a reload
            showDialog(DIALOG_TEXT_MODULES);
        } else if (textModules.size() == 1) {
            for (Map.Entry<String, String> stringStringEntry : textModules.entrySet()) {
                textField.insertText(stringStringEntry.getValue()); //its only one in this loop, so do not break
            }
        } else if (textModules.size() == 0) {
            Toast toast = Toast.makeText(this, R.string.no_text_modules, Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * check if a receiver is chosen and text was set before sending
     *
     * @return
     */
    private boolean preSendCheck() {
        String toastMessage = "";
        String textPostValidation = receiverField.getText().toString();
        receiverField.performValidation();
        String textPreValidation = receiverField.getText().toString();
        if (!textPostValidation.equals(textPreValidation)) {
            toastMessage += (toastMessage.length() != 0) ? "\n" : "";
            toastMessage += getString(R.string.inputs_corrected);
            if (receiverField.getReceiverList().size() == 0) { //set the text empty if after validation no receiver is availabel anymore
                receiverField.setText("");
            }
        }
        if (receiverField.getReceiverList().size() == 0) {
            String patchResult = InputPatcher.patchProgram(textField.getText().toString(), smSoIPPlugin.getProvider());
            if (patchResult != null) {
                toastMessage += patchResult;
            } else {
                toastMessage += (toastMessage.length() != 0) ? "\n" : "";
                toastMessage += getString(R.string.noNumberInput);
            }
        }
        if (textField.getText().toString().trim().length() == 0) {
            toastMessage += (toastMessage.length() != 0) ? "\n" : "";
            toastMessage += getString(R.string.noTextInput);
        }
        String spinnerText = spinner.getVisibility() == View.INVISIBLE || spinner.getVisibility() == View.GONE ? null : spinner.getSelectedItem().toString();
        if (smSoIPPlugin.isTimeShiftCapable(spinnerText) && dateTime != null) {
            if (dateTime.getCalendar().before(Calendar.getInstance())) {
                toastMessage += (toastMessage.length() != 0) ? "\n" : "";
                toastMessage += getString(R.string.time_in_past);
            }
        }
        if (toastMessage.length() > 0) {
            Toast toast = Toast.makeText(this, toastMessage, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }

        return true;
    }

    /**
     * set the button for refreshing informations
     */
    private void setRefreshButton() {
        View refreshButon = findViewById(R.id.refreshButton);
        View infoTextUpper = findViewById(R.id.infoTextUpper);
        View.OnClickListener l = new View.OnClickListener() {
            public void onClick(View view) {
                ErrorReporterStack.put(LogConst.REFRESH_CLICKED);
                refreshInformationText(true);
            }
        };
        refreshButon.setOnClickListener(l);
        infoTextUpper.setOnClickListener(l);


    }

    /**
     * set the button for clear all inputs
     */
    private void setClearButton() {
        ImageButton clearButton = (ImageButton) findViewById(R.id.clearButton);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getText(R.string.wantClear))
                .setPositiveButton(getText(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        clearAllInputs();
                    }
                });
        final AlertDialog alert = builder.create();
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Map<String, String> build = MapBuilder.createEvent(CAT_BUTTONS, EVENT_CLEAR, "", null).build();
                EasyTracker.getInstance(SendActivity.this).send(build);
                if (!textField.getText().toString().equals("") || !receiverField.getText().toString().equals("")) {
                    alert.show();
                } else {
                    Toast toast = Toast.makeText(SendActivity.this, R.string.nothing_to_clear, Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    /**
     * clear the text and the receivers
     */
    private void clearAllInputs() {
        receiverField.clearReceiverList();
        textField.setText("");
        resetDateTimePicker();
        updateViewOnChangedReceivers();
        setSuppliersLayout();
    }

    private void resetDateTimePicker() {
        View timeShiftLayout = findViewById(R.id.timeShiftLayout);
        if (timeShiftLayout.getVisibility() == View.VISIBLE) {
            dateTime = null;
            CheckBox pickTimeCheckBox = (CheckBox) findViewById(R.id.pickTime);
            pickTimeCheckBox.setChecked(false);  //calls the listener
        }
    }

    /**
     * called after sending was successful to write message in devices database
     *
     * @param receiverList
     */
    private void writeSMSInDatabase(List<Receiver> receiverList) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean writeToDatabaseEnabled = settings.getBoolean(SettingsConst.GLOBAL_WRITE_TO_DATABASE, false) && SMSoIPApplication.getApp().isWriteToDatabaseAvailable();
        if (writeToDatabaseEnabled) {
            String message = "";
            if (settings.getBoolean(SettingsConst.GLOBAL_ENABLE_PROVIDER_OUPUT, false)) {
                OptionProvider provider = smSoIPPlugin.getProvider();

                if (provider.getAccounts().size() > 1) {
                    message = settings.getString(SettingsConst.OUTPUT_TEMPLATE_MULTI, "%a (%u->%p):" + " ");
                } else {
                    message = settings.getString(SettingsConst.OUTPUT_TEMPLATE_SINGLE, "%a (%p):") + " ";
                }
                message = message.replaceAll("%a", getString(R.string.applicationName)).replaceAll("%u", provider.getUserName()).replaceAll("%p", provider.getProviderName());
            }
            message += textField.getText();
            AndroidInternalDatabaseHandler.writeSMSInDatabase(receiverList, message, dateTime, this);
        }
    }

    /**
     * set the sms text area
     */
    private void setTextArea() {
        textField = (SMSInputEditText) findViewById(R.id.textInput);
        textField.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (smSoIPPlugin != null) { //activity was resumed
                    updateSMScounter();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                textField.processReplacement();
            }
        });

    }

    /**
     * update the sms counter top of the sms text area
     */
    public void updateSMScounter() {
        Editable charSequence = textField.getText();
        OptionProvider provider = smSoIPPlugin.getProvider();
        int messageLength = provider.getTextMessageLength();
        int maxMessageCount = provider.getMaxMessageCount();

        InputFilter[] fArray = new InputFilter[1];
        fArray[0] = new InputFilter.LengthFilter(maxMessageCount * messageLength);
        textField.setFilters(fArray);

        int textLength = charSequence.length();
        int smsCount = provider.getLengthDependentSMSCount(textLength);
        if (textLength != 0) {
            if (smsCount == 0) {
                smsCount = Math.round((textLength / messageLength));
                smsCount = textLength % messageLength == 0 ? smsCount : smsCount + 1;
            }
            if (smsCount > maxMessageCount) {
                smssigns.setTextColor(Color.rgb(255, 0, 0));
            } else {
                smssigns.setTextColor(getDefaultColor());
            }
        } else {
            smssigns.setTextColor(getDefaultColor());
        }
        smssigns.setText(String.format(getText(R.string.smssigns).toString(), textLength, smsCount));
    }

    /**
     * set the button by searching for receivers
     */
    private void setSearchButton() {
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT_REQUEST);
            }
        });

        searchButton.setVisibility(SMSoIPApplication.getApp().isPickActionAvailable() ? View.VISIBLE : View.GONE);

    }

    /**
     * set the spinner if plugin has one
     */
    private void setSpinner() {
        spinner = (Spinner) findViewById(R.id.typeSpinner);
        smSoIPPlugin.getProvider().createSpinner(this, spinner);
        findViewById(R.id.typeText).setVisibility(spinner.getVisibility());
        //force set again with animated = true to force a repaint
        spinner.setSelection(spinner.getSelectedItemPosition(), true);
    }


    /**
     * will be called for sending in a thread to update progress dialog
     *
     * @return
     */
    public FireSMSResultList sendTextMessage() {
        ErrorReporterStack.put(LogConst.SEND_BY_THREAD + smSoIPPlugin.getProviderName());
        CheckForDuplicatesArrayList receiverList = receiverField.getReceiverList();
        String spinnerText = spinner.getVisibility() == View.INVISIBLE || spinner.getVisibility() == View.GONE ? null : spinner.getSelectedItem().toString();
        OptionProvider provider = smSoIPPlugin.getProvider();
        String userName = provider.getUserName();
        String pass = provider.getPassword();
        if (provider.hasAccounts() && provider.isCheckLoginButtonVisible()) {
            if (userName == null || userName.trim().length() == 0 || pass == null || pass.trim().length() == 0) {
                return FireSMSResultList.getAllInOneResult(SMSActionResult.NO_CREDENTIALS(), receiverList);
            }
        }
        FireSMSResultList out;
        Tracker tracker = EasyTracker.getInstance(this);
        try {
            Map<String, String> build;
            if (smSoIPPlugin.isTimeShiftCapable(spinnerText) && dateTime != null) {
                build = MapBuilder.createEvent(CAT_SEND, EVENT_TIMESHIFT, smSoIPPlugin.getProviderName(), null).build();
                out = smSoIPPlugin.getTimeShiftSupplier().fireTimeShiftSMS(textField.getText().toString(), receiverList, spinnerText, dateTime);
            } else {
                build = MapBuilder.createEvent(CAT_SEND, EVENT_NORMAL, smSoIPPlugin.getProviderName(), null).build();
                out = smSoIPPlugin.getSupplier().fireSMS(textField.getText().toString(), receiverList, spinnerText);
            }
            EasyTracker.getInstance(SendActivity.this).send(build);
        } catch (UnsupportedEncodingException e) {
            tracker.send(MapBuilder.createException(CAT_SEND + "" + e.getMessage(), false).build());
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receiverList);
        } catch (NumberFormatException e) {
            tracker.send(MapBuilder.createException(CAT_SEND + "" + e.getMessage(), false).build());
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receiverList);
        } catch (SocketTimeoutException e) {
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.TIMEOUT_ERROR(), receiverList);
        } catch (IOException e) {
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.NETWORK_ERROR(), receiverList);
        } catch (Exception e) {                                                      //for insurance
            ACRA.getErrorReporter().handleSilentException(e);
            tracker.send(MapBuilder.createException(CAT_SEND + "" + e.getMessage(), true).build());
            out = FireSMSResultList.getAllInOneResult(SMSActionResult.UNKNOWN_ERROR(), receiverList);
        }

        return out;
    }


    /**
     * refresh the information text
     *
     * @param refreshButtonPressed by refresh button or after send?
     */

    public void refreshInformationText(Boolean refreshButtonPressed) {
        ErrorReporterStack.put(LogConst.REFRESH_INFORMATION_TEXT + smSoIPPlugin.getProviderName());
        cancelUpdateTask();
        showUpdateProgressBar();
        backgroundUpdateTask = new BackgroundUpdateTask(this).execute(refreshButtonPressed);
    }

    /**
     * returning point after returning from choose contact from device
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            Uri contactData = data.getData();
            final Contact pickedContact = AndroidInternalDatabaseHandler.getPickedContactData(contactData, this);

            if (!pickedContact.getNumberTypeList().isEmpty()) { //nothing picked or no number
                //always one contact, so it will be filled always


                LinkedList<BasicNameValuePair> numberTypeList = pickedContact.getNumberTypeList();
                final Receiver receiver = new Receiver(pickedContact.getName());
                if (numberTypeList.size() == 1) { //only one number, so choose this
                    BasicNameValuePair entry = numberTypeList.getFirst();
                    receiver.setRawNumber(entry.getName(), entry.getValue());
                    addReceiver(receiver);

                } else { //more than one number for contact
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setTitle(String.format(getString(R.string.pickNumber), pickedContact.getName()));
                    //build a map of string on screen with corresponding number for layout
                    final Map<String, String> presentationMap = new HashMap<String, String>();
                    for (BasicNameValuePair basicNameValuePair : numberTypeList) {
                        presentationMap.put(basicNameValuePair.getName() + " (" + basicNameValuePair.getValue() + ")", basicNameValuePair.getName());
                    }
                    final String[] items = presentationMap.keySet().toArray(new String[presentationMap.size()]);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String key = null;//get the picked number back from origin map
                            for (Map.Entry<String, String> entry : presentationMap.entrySet()) {
                                if (entry.getKey().equals(items[item])) {
                                    key = entry.getValue();
                                    break;
                                }
                            }
                            receiver.setRawNumber(key, getString(R.string.no_phone_type_label));
                            addReceiver(receiver);
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);


                builder.setMessage(String.format(getText(R.string.noNumber).toString(), pickedContact.getName()))
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });


                AlertDialog alert = builder.create();
                alert.show();
            }
        }

    }

    /**
     * add a receiver and check all prerequisites for adding
     *
     * @param receiver
     */
    private void addReceiver(Receiver receiver) {
        ErrorReporterStack.put(LogConst.ADD_TO_RECEIVER);
        CheckForDuplicatesArrayList receiverList = receiverField.getReceiverList();
        if (smSoIPPlugin == null || receiverList.size() < smSoIPPlugin.getProvider().getMaxReceiverCount()) {  //check only if smsoipPlugin is already set
            if (receiverList.addWithAlreadyInsertedCheck(receiver)) {
                showAddedTwiceToast();
            }
            receiverField.updateTextContent();//is already added by addWithAlreadyInsertedCheck
            if (smSoIPPlugin != null) { //update only if plugin already set
                updateViewOnChangedReceivers();
            }
        } else {
            String text = String.format(getText(R.string.max_receivers_reached).toString(), smSoIPPlugin.getProvider().getMaxReceiverCount());
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
        }
    }

    /**
     * toast as hint if a receiver is added twice
     */
    private void showAddedTwiceToast() {
        Toast toast = Toast.makeText(this, R.string.receiver_added_twice, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * update all stuff if a receiver was added (by receivertext, notification or selecting from contact)
     */
    private void updateViewOnChangedReceivers() {
        //remove all disabled providers
        CheckForDuplicatesArrayList receiverList = receiverField.getReceiverList();
        for (Iterator<Receiver> iterator = receiverList.iterator(); iterator.hasNext(); ) {
            Receiver next = iterator.next();
            if (!next.isEnabled()) {
                iterator.remove();
            }
        }
        receiverField.setMaxReceivers(smSoIPPlugin.getProvider().getMaxReceiverCount());
        receiverField.updateTextContent();//if some is removed
        //update the marking of textfield
        setVisibilityByCurrentReceivers();
        setInfoButtonVisibility();
        setDateTimePickerDialog();
    }

    /**
     * set the layout of the current selected supplier
     */
    private void setSuppliersLayout() {
        LinearLayout freeLayout = (LinearLayout) findViewById(R.id.freeLayout);
        freeLayout.removeAllViews();
        freeLayout.setOrientation(LinearLayout.HORIZONTAL);
        if (smSoIPPlugin != null) {
            smSoIPPlugin.getProvider().getFreeLayout(freeLayout);
        }
    }

    /**
     * set the visibility of chosse contact button by current plugin and receiver count
     */
    private void setVisibilityByCurrentReceivers() {
        View viewById = findViewById(R.id.showChosenContacts);
        List receiverList = receiverField.getReceiverList();
        if (receiverList.size() > 0) {
            viewById.setVisibility(View.VISIBLE);
        } else {
            viewById.setVisibility(View.GONE);
        }
        View searchButton = findViewById(R.id.searchButton);
        if (smSoIPPlugin != null && receiverList.size() >= smSoIPPlugin.getProvider().getMaxReceiverCount()) {
            searchButton.setVisibility(View.GONE);
        } else if (SMSoIPApplication.getApp().isPickActionAvailable()) {
            searchButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * set the visibility of the info button dependent of the last info dialog
     */
    private void setInfoButtonVisibility() {
        View showInfoButton = findViewById(R.id.showInfoButton);
        if (lastInfoDialogContent != null && result != null) {
            showInfoButton.setVisibility(View.VISIBLE);
        } else {
            showInfoButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    /**
     * entry point when menu button is clicked
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, PROVIDER_OPTION, Menu.CATEGORY_SECONDARY, R.string.provider_settings);
        item.setIcon(R.drawable.ic_menu_manage).setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        MenuItem globalOption = menu.add(0, GLOBAL_OPTION, Menu.CATEGORY_SECONDARY, R.string.program_settings);
        globalOption.setIcon(R.drawable.ic_menu_compose);
        boolean actionBarButtonsVisible = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsConst.GLOBAL_BUTTON_VISIBILITY, true);
        boolean providerChangeVisibility = SMSoIPApplication.getApp().getProviderEntries().size() > 1;
        if (providerChangeVisibility && actionBarButtonsVisible) {
            MenuItem switchSupplier = menu.add(0, OPTION_SWITCH_SUPPLIER, Menu.CATEGORY_SYSTEM, R.string.changeProvider);
            switchSupplier.setIcon(R.drawable.ic_menu_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        if (smSoIPPlugin != null && smSoIPPlugin.getProvider().getAccounts().size() > 1 && actionBarButtonsVisible) {
            MenuItem switchAccount = menu.add(0, OPTION_SWITCH_ACCOUNT, Menu.CATEGORY_SYSTEM, R.string.changeAccount);
            switchAccount.setIcon(R.drawable.ic_menu_rotate).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    /**
     * entry point when option is clicked
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        Tracker tracker = EasyTracker.getInstance(this);
        Map<String, String> build;
        switch (item.getItemId()) {
            case PROVIDER_OPTION:
                build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(PROVIDER_OPTION), LABEL_MENU, null).build();
                tracker.send(build);
                startOptionActivity();
                return true;
            case OPTION_SWITCH_SUPPLIER:
                build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(OPTION_SWITCH_SUPPLIER), LABEL_MENU, null).build();
                tracker.send(build);
                showProvidersDialog();
                return true;
            case GLOBAL_OPTION:
                build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(GLOBAL_OPTION), LABEL_MENU, null).build();
                tracker.send(build);
                startGlobalOptionActivity();
                return true;
            case OPTION_SWITCH_ACCOUNT:
                build = MapBuilder.createEvent(CAT_OPTIONS, String.valueOf(OPTION_SWITCH_ACCOUNT), LABEL_MENU, null).build();
                tracker.send(build);
                showAccountDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * show a dialog of the accounts of this provider (count dependent)
     */
    private void showAccountDialog() { //only available if more than one is available
        OptionProvider provider = smSoIPPlugin.getProvider();
        Map<Integer, AccountModel> accounts = provider.getAccounts();
        if (accounts.size() > 2) {
            removeDialog(DIALOG_SWITCH_ACCOUNT); //remove the chosenContactsDialog forces recreation
            showDialog(DIALOG_SWITCH_ACCOUNT);
        } else { //only one  other than the current
            Integer accountIndex = provider.getCurrentAccountIndex();
            for (Integer accountId : accounts.keySet()) {
                if (accountIndex != null && accountId != null && !accountIndex.equals(accountId)) {
                    switchAccount(accountId);
                    break;
                }
            }
        }
    }

    /**
     * start the activity for program settings
     */
    private void startGlobalOptionActivity() {
        Intent pref = new Intent(this, GlobalPreferences.class);
        View rootLayout = findViewById(R.id.rootLayout);
        int height = getWindow().getDecorView().getHeight() - rootLayout.getHeight();
        pref.putExtra(SettingsConst.EXTRA_ADJUSTMENT, height);
        startActivity(pref);
        optionsCalled = true;
    }

    /**
     * start the activity for the suppliers option
     */
    private void startOptionActivity() {
        Intent intent = new Intent(this, ProviderPreferences.class);
        intent.putExtra(ProviderPreferences.SUPPLIER_CLASS_NAME, smSoIPPlugin.getSupplierClassName());
        startActivity(intent);
        optionsCalled = true;
        providerOptionsCalled = true;
    }


    @Override
    /**
     * entry point for showing every dialog in this activity
     */
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        ErrorReporterStack.put(LogConst.ON_CREATE_DIALOG + id);
        Map<String, String> build = MapBuilder.createEvent(CAT_BUTTONS, String.valueOf(id), "", null).build();
        EasyTracker.getInstance(this).send(build);
        switch (id) {
            case DIALOG_TEXT_MODULES:
                Map<String, String> modules = textField.getTextModules();
                final CharSequence[] textModules = modules.values().toArray(new CharSequence[modules.size()]);

                AlertDialog.Builder textModulesBuilder = new AlertDialog.Builder(this);
                textModulesBuilder.setItems(textModules, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        textField.insertText(textModules[item].toString());
                        try {
                            dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                            ACRA.getErrorReporter().handleSilentException(e);
                        }

                    }
                });
                dialog = textModulesBuilder.create();
                break;
            case DIALOG_SMILEYS:
                dialog = new SmileyDialog(this);

                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        SmileyDialog smileyDialog = (SmileyDialog) dialog;
                        if (smileyDialog.isPositiveResult()) {
                            String itemString = smileyDialog.getItem();
                            textField.insertText(itemString);
                        }
                    }
                });
                break;
            case DIALOG_PROVIDER://this will only called if more than two providers are available, otherwise dialog will be null
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                final ChangeProviderArrayAdapter adapter = new ChangeProviderArrayAdapter(this, smSoIPPlugin);
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        String supplierClassName = adapter.getItem(item).getSupplierClassName();
                        try {
                            dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                            ACRA.getErrorReporter().handleSilentException(e);
                        }
                        changeSupplier(supplierClassName);
                    }
                };

                builder.setAdapter(adapter, listener);
                builder.setTitle(R.string.chooseProvider);
                builder.setCancelable(adapter.isCancelable()); //only cancelable on switch providers
                dialog = builder.create();
                break;
            case DIALOG_SWITCH_ACCOUNT:
                OptionProvider provider = smSoIPPlugin.getProvider();
                Map<Integer, AccountModel> accounts = provider.getAccounts();
                Map<Integer, AccountModel> filteredAccounts = new HashMap<Integer, AccountModel>();
                Integer currentAccount = provider.getCurrentAccountIndex();
                //filter list by current
                for (Map.Entry<Integer, AccountModel> next : accounts.entrySet()) {
                    if (next.getKey().equals(currentAccount)) {
                        continue;
                    }
                    filteredAccounts.put(next.getKey(), next.getValue());
                }

                builder = new AlertDialog.Builder(this);
                CharSequence[] items = new CharSequence[filteredAccounts.size()];
                int i = 0;
                final Map<Integer, Integer> charAccountRel = new HashMap<Integer, Integer>(filteredAccounts.size());
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : filteredAccounts.entrySet()) {
                    items[i] = integerAccountModelEntry.getValue().getUserName();
                    charAccountRel.put(i++, integerAccountModelEntry.getKey());
                }
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        switchAccount(charAccountRel.get(item));
                    }
                });
                builder.setTitle(R.string.chooseAccount);
                dialog = builder.create();
                break;
            default:
                dialog = super.onCreateDialog(id);
        }
//        lastDialog = dialog;
        return dialog;
    }

    /**
     * do all Gui stuff on switching the account
     *
     * @param accountId
     */
    private void switchAccount(Integer accountId) {
        resetInfoText();
        cancelUpdateTask();
        smSoIPPlugin.getProvider().setCurrentAccountId(accountId);
        setFullTitle();
        receiverField.setMaxReceivers(smSoIPPlugin.getProvider().getMaxReceiverCount());
        updateInfoTextSilent();
    }

    /**
     * cancel the update task if set
     */
    private void cancelUpdateTask() {
        if (backgroundUpdateTask != null) {
            if (backgroundUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
                resetInfoText();
            }
            backgroundUpdateTask.cancel(true);
        }
    }

    /**
     * do all Gui stuff on switching the supplier
     *
     * @param supplierClassName
     */
    private void changeSupplier(String supplierClassName) {
        resetInfoText();
        cancelUpdateTask();
        smSoIPPlugin = SMSoIPApplication.getApp().getSMSoIPPluginBySupplierName(supplierClassName);
        ErrorReporterStack.put(LogConst.CHANGE_SUPPLIER + smSoIPPlugin.getProviderName());
        smSoIPPlugin.getProvider().refresh();//refresh the provider to set back to the default account
        receiverField.setMaxReceivers(smSoIPPlugin.getProvider().getMaxReceiverCount());
        setFullTitle();
        //reset all not needed informations
        updateSMScounter();
        setSpinner();
        setDateTimePickerDialog();
        updateAfterReceiverCountChanged();
        updateInfoTextSilent();
        invalidateOptionsMenu();
        setViewByMode(mode);
    }

    /**
     * do all Gui stuff after receiver has changed (inc and dec)
     */
    public void updateAfterReceiverCountChanged() {
        int maxReceiverCount = smSoIPPlugin.getProvider().getMaxReceiverCount();
        CheckForDuplicatesArrayList receiverList = receiverField.getReceiverList();
        if (receiverList.size() > maxReceiverCount) {
            CheckForDuplicatesArrayList newReceiverList = new CheckForDuplicatesArrayList();
            for (int i = 0; i < maxReceiverCount; i++) {
                newReceiverList.add(receiverList.get(i));

            }
            receiverField.setReceiverList(newReceiverList);
            updateViewOnChangedReceivers();
            showTooMuchReceiversToast();
        } else {
            updateViewOnChangedReceivers();
        }
        setSuppliersLayout();
    }

    /**
     * show a hint if too much receivers are shown
     */
    private void showTooMuchReceiversToast() {
        if (smSoIPPlugin != null) { //can be null on startup
            String text = String.format(getText(R.string.too_much_receivers).toString(), smSoIPPlugin.getProvider().getMaxReceiverCount());
            Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
        }
    }


    @Override
    /**
     * entry point when search button is pressed
     */
    public boolean onSearchRequested() {
        View searchButton = findViewById(R.id.searchButton);

        if (searchButton.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT_REQUEST);
        }
        return false;
    }


    @Override
    /**
     * entry point when activity going to sleep
     */
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //cancel the update if running
        cancelUpdateTask();
        if (smSoIPPlugin != null) { //only save instance if provider is already chosen
            outState.putString(SAVED_INSTANCE_SUPPLIER, smSoIPPlugin.getSupplierClassName());
            outState.putCharSequence(SAVED_INSTANCE_INPUTFIELD, receiverField.getText());
            outState.putParcelableArrayList(SAVED_INSTANCE_RECEIVERS, receiverField.getReceiverList());
            CharSequence infoText = ((TextView) findViewById(R.id.infoText)).getText();
            outState.putCharSequence(SAVED_INSTANCE_INFO, infoText);
            outState.putInt(SAVED_INSTANCE_MODE, mode.ordinal());
            currentAccountIndex = smSoIPPlugin.getProvider().getCurrentAccountIndex();
            if (currentAccountIndex != null) {
                outState.putInt(SAVED_INSTANCE_ACCOUNT_ID, currentAccountIndex);
            }
            if (dateTime != null) {
                outState.putLong(SAVED_INSTANCE_DATE_TIME, dateTime.getCalendar().getTimeInMillis());
            }
            if (spinner.getVisibility() == View.VISIBLE) {
                outState.putInt(SAVED_INSTANCE_SPINNER, spinner.getSelectedItemPosition());
            }
            if (lastInfoDialogContent != null) {
                outState.putString(SAVED_INSTANCE_LAST_INFO_DIALOG_CONTENT, lastInfoDialogContent);
            }
            if (result != null) {
                outState.putString(SAVED_INSTANCE_LAST_INFO_DIALOG_RESULT, result.name());
            }
            smSoIPPlugin.getProvider().onActivityPaused(outState);
        }
    }


    /**
     * show the returning message after sending in a dialog
     *
     * @param fireSMSResults
     */
    public void showReturnMessage(FireSMSResultList fireSMSResults) {

        StringBuilder resultMessage = new StringBuilder();
        if (fireSMSResults.size() == 1) {  // nobody cares about extra Infos if only one message was sent
            resultMessage.append(fireSMSResults.get(0).getResult().getMessage());
        } else {
            String unknownReceiverText = getText(R.string.unknown).toString();
            for (int i = 0, fireSMSResultsSize = fireSMSResults.size(); i < fireSMSResultsSize; i++) {
                FireSMSResult fireSMSResult = fireSMSResults.get(i);
                Receiver receiver = fireSMSResult.getReceiver();
                resultMessage.append("<b><u>");
                if (unknownReceiverText.equals(receiver.getName())) {
                    resultMessage.append(receiver.getReceiverNumber());
                } else {
                    resultMessage.append(receiver.getName());
                }
                resultMessage.append("</u></b>");
                resultMessage.append("<br/>").append(fireSMSResult.getResult().getMessage());
                if (i != fireSMSResultsSize - 1) {
                    resultMessage.append("<br/>");
                }
            }
        }

        result = fireSMSResults.getResult();
        if (!this.isFinishing()) {
            lastInfoDialogContent = resultMessage.toString();
            EmoImageDialog lastInfoDialog = new EmoImageDialog(this, result, lastInfoDialogContent);
            lastInfoDialog.show();
            ThreadingUtil.killDialogAfterAWhile(lastInfoDialog);
        }
        writeSMSInDatabase(fireSMSResults.getSuccessList());
        if (result == FireSMSResultList.SendResult.SUCCESS) {
            clearAllInputs();
        } else {
            receiverField.getReceiverList().removeAll(fireSMSResults.getSuccessList());
            updateViewOnChangedReceivers();
        }
    }

    @Override
    /**
     * entry point if application is already open
     */
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (smSoIPPlugin != null) {   // causes NPE, if no provider is currently selected and app is open (dialog shown)
            clearAllInputs();
        }
        IntentHandler handler = new IntentHandler(intent, this);
        setPreselectedContact(handler);
    }

    /**
     * get the current selected plugin
     *
     * @return
     */
    public SMSoIPPlugin getSmSoIPPlugin() {
        return smSoIPPlugin;
    }

    /**
     * helper for the default text color
     *
     * @return
     */
    public ColorStateList getDefaultColor() {
        if (defaultColor == null) {
            defaultColor = new TextView(getApplicationContext()).getTextColors();
        }
        return defaultColor;
    }

    @Override
    protected void onDestroy() {
        ((AdLayout) findViewById(R.id.adLayoutLower)).destroy();
        ((AdLayout) findViewById(R.id.adLayoutUpper)).destroy();
        super.onDestroy();
    }
}
