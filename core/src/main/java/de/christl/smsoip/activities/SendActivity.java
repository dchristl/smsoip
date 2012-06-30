package de.christl.smsoip.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.FireSMSResult;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.database.DatabaseHandler;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.patcher.InputPatcher;
import de.christl.smsoip.provider.SMSSupplier;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.ui.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SendActivity extends AllActivity {

    private EditText inputField;
    private EditText textField;
    TextView smssigns;
    private Spinner spinner;

    private static final int PICK_CONTACT_REQUEST = 0;

    public CharSequence SIGNSCONSTANT;
    private ProgressDialog progressDialog;


    public Toast toast;
    private SMSSupplier smsSupplier;
    private static final int PROVIDER_OPTION = 30;
    private static final int OPTION_SWITCH_SUPPLIER = 31;
    private static final int DIALOG_SMILEYS = 32;
    private static final int DIALOG_PROVIDER = 33;
    private static final int GLOBAL_OPTION = 34;
    private static final int DIALOG_NUMBER_INPUT = 35;
    private static final int OPTION_SWITCH_ACCOUNT = 36;
    private static final int DIALOG_SWITCH_ACCOUNT = 37;

    private SharedPreferences settings;
    CheckForDuplicatesArrayList receiverList = new CheckForDuplicatesArrayList();
    private View addContactbyNumber;
    private ImageButton searchButton;
    private ChosenContactsDialog chosenContactsDialog;
    private static final String SAVED_INSTANCE_SUPPLIER = "supplier";
    private static final String SAVED_INSTANCE_INPUTFIELD = "inputfield";
    private static final String SAVED_INSTANCE_RECEIVERS = "receivers";
    private static final String SAVED_INSTANCE_SPINNER = "spinner";
    private static final String SAVED_INSTANCE_INFO = "info";
    private static final String SAVED_INSTANCE_ACCOUNT_ID = "account";

    private Dialog lastDialog;
    private static final String TAG = SendActivity.class.getCanonicalName();
    private boolean optionsCalled = false;
    private Dialog lastInfoDialog;

    @Override
    protected void onResume() {
        super.onResume();
        //this is for performance cases and can cause issues in some case:
        // if options are called a refresh will be forced because settings can change
        // if activity is killed a new instance will be creeated automatically (and options are "fresh")
        // refresh will only be called if kill not happens
        // otherwise saved instance states are overwritten
        if (smsSupplier != null && optionsCalled) {
            smsSupplier.getProvider().refresh();
            settings = PreferenceManager.getDefaultSharedPreferences(this);
            setFullTitle();
            optionsCalled = true;
        } else if (smsSupplier == null) {
            Log.e(TAG, "SMSSupplier is null on resume");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.sendactivity);
        SIGNSCONSTANT = getText(R.string.text_smssigns);
        inputField = (EditText) findViewById(R.id.numberInput);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        smssigns = (TextView) findViewById(R.id.smssigns);
        smssigns.setText(String.format(SIGNSCONSTANT.toString(), 0, 0));
        toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        //disable inputs on field
        inputField.setKeyListener(null);
        setSearchButton();
        setClearButton();
        setRefreshButton();
        setSigButton();
        setShowChosenContactsDialog();
        setShortTextButton();
        setSmileyButton();
        setTextArea();
        setSendButton();
        setContactsByNumberInput();
        setPreselectedContact();
        setLastInfoButton();
        if (savedInstanceState != null && savedInstanceState.getString(SAVED_INSTANCE_SUPPLIER) != null) {  //activity was killed and is resumed
            smsSupplier = SMSoIPApplication.getApp().getInstance(savedInstanceState.getString(SAVED_INSTANCE_SUPPLIER));
            setFullTitle();
            setSpinner();
            if (spinner.getVisibility() == View.VISIBLE) { //if the spinner is visible, the  spinner item is selected, too
                spinner.setSelection(savedInstanceState.getInt(SAVED_INSTANCE_SPINNER, 0));
            }
            inputField.setText(savedInstanceState.getCharSequence(SAVED_INSTANCE_INPUTFIELD));
            ArrayList<Receiver> tmpReceiverList = savedInstanceState.getParcelableArrayList(SAVED_INSTANCE_RECEIVERS);
            receiverList = new CheckForDuplicatesArrayList(); //simple copy, cause of unknown compile error
            receiverList.addAll(tmpReceiverList);
            int accountIndex = savedInstanceState.getInt(SAVED_INSTANCE_ACCOUNT_ID);
            switchAccount(accountIndex);
            updateInfoTextAndRefreshButton(savedInstanceState.getString(SAVED_INSTANCE_INFO));
            updateViewOnChangedReceivers(); //call it if a a receiver is appended
        } else {     // fresh create call on activity so do the default behaviour
            String defaultSupplier = getDefaultSupplier();
            if (defaultSupplier != null) {
                smsSupplier = SMSoIPApplication.getApp().getInstance(defaultSupplier);
                setFullTitle();
                setSpinner();
                updateViewOnChangedReceivers();
            } else {
                removeDialog(DIALOG_PROVIDER);
                showDialog(DIALOG_PROVIDER);
            }
            updateInfoTextSilent();
        }
        insertAds(R.id.banner_adview, this);

    }

    private void updateInfoTextSilent() {
        //only if parameter and supplier set
        if (settings.getBoolean(GlobalPreferences.GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP, true) && smsSupplier != null) {
            final View refreshButton = findViewById(R.id.resfreshButton);
            refreshButton.setEnabled(false);
            final TextView infoText = (TextView) findViewById(R.id.infoText);
            infoText.setText(R.string.text_notyetrefreshed);
            RunnableFactory factory = new RunnableFactory(this, null);
            factory.updateInfoTextInBackground();

        }


    }

    public void updateInfoTextAndRefreshButton(String info) {
        if (info != null) {
            ((TextView) findViewById(R.id.infoText)).setText(info);
        }
        findViewById(R.id.resfreshButton).setEnabled(true);
    }

    private void setLastInfoButton() {
        View showInfoButton = findViewById(R.id.showInfoButton);
        showInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastInfoDialog != null) {
                    lastInfoDialog.show();
                    killDialogAfterAWhile(lastInfoDialog);
                }
            }
        });
    }

    private void setFullTitle() {
        OptionProvider provider = smsSupplier.getProvider();
        String userName = provider.getUserName() == null ? getString(R.string.text_account_no_account) : provider.getUserName();
        setTitle(provider.getProviderName() + " (" + userName + ")");
    }

    private void setPreselectedContact() {
        Uri data = getIntent().getData();

        if (data != null) {
            DatabaseHandler dbHandler = new DatabaseHandler(this);
            String givenNumber = data.getSchemeSpecificPart();
            Receiver contactByNumber = dbHandler.findContactByNumber(givenNumber);
            if (contactByNumber == null) {
                contactByNumber = new Receiver("-1", getText(R.string.text_unknown).toString(), 0);
                contactByNumber.addNumber(givenNumber, getText(R.string.text_unknown).toString());
            }
            String number = contactByNumber.getFixedNumberByRawNumber(givenNumber);
            contactByNumber.setReceiverNumber(number);
            if (receiverList.addWithAlreadyInsertedCheck(contactByNumber)) {
                toast.setText(R.string.text_receiver_added_twice);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
                toast.show();
            }
        }
    }

    private void setSendButton() {

        Button sendButton = (Button) findViewById(R.id.sendButton);
        final CharSequence progressText = getText(R.string.text_smscomitted);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showLastMessagesDialog();
                if (!preSendCheck() || progressDialog == null) {
                    return;
                }
                progressDialog.setMessage(progressText);
                progressDialog.show();
                if (SMSoIPApplication.getApp().getMinAPIVersion(smsSupplier) >= 14) {
                    new Thread(new RunnableFactory(SendActivity.this, progressDialog).getFireSMSAndUpdateUIRunnable()).start();
                } else {
                    new Thread(new RunnableFactory(SendActivity.this, progressDialog).getSendAndUpdateUIRunnable()).start();
                }
            }
        }

        );
    }

    private void showLastMessagesDialog() {
        ShowLastMessagesDialog lastMessageDialog = new ShowLastMessagesDialog(this, receiverList);
        lastMessageDialog.show();
    }


    private String getDefaultSupplier() {
        String string = settings.getString(GlobalPreferences.GLOBAL_DEFAULT_PROVIDER, "");
        //check if default provider is installed
        if (!string.equals("")) {
            boolean found = false;
            for (ProviderEntry providerEntry : SMSoIPApplication.getApp().getProviderEntries().values()) {
                if (providerEntry.getSupplierClassName().equals(string)) {
                    found = true;
                    break;
                }
            }
            if (!found) { //set back to default (always ask) if none found
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(GlobalPreferences.GLOBAL_DEFAULT_PROVIDER, null);
                editor.commit();
                string = null;
            }
        } else {
            string = null;
        }

        return string;
    }

    private void setContactsByNumberInput() {
        addContactbyNumber = findViewById(R.id.addcontactbynumber);

        addContactbyNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(DIALOG_NUMBER_INPUT);
            }
        });

    }

    private void setShowChosenContactsDialog() {
        ImageButton chosenContactsdialogButton = (ImageButton) findViewById(R.id.showChosenContacts);
        chosenContactsdialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChosenContactsDialog();
            }
        });
    }

    private void showChosenContactsDialog() {
        chosenContactsDialog = new ChosenContactsDialog(this, receiverList);
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (chosenContactsDialog != null && chosenContactsDialog.isShowing()) {
            chosenContactsDialog.redraw();
        }
    }

    private void setSmileyButton() {
        ImageButton smileyButton = (ImageButton) findViewById(R.id.insertSmileyButton);
        smileyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(DIALOG_SMILEYS);
            }
        });

    }

    private void setShortTextButton() {
        ImageButton shortTextButton = (ImageButton) findViewById(R.id.shortTextButton);
        shortTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String oldText = textField.getText().toString();
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
            }
        });
    }

    private void setSigButton() {
        ImageButton sigButton = (ImageButton) findViewById(R.id.insertSigButton);
        sigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textField.setText(textField.getText() + " " + settings.getString(GlobalPreferences.GLOBAL_SIGNATURE, "Sent by SMSoIP"));
                int position = textField.length();
                Editable etext = textField.getText();
                Selection.setSelection(etext, position);
            }
        });
    }


    private boolean preSendCheck() {
        String toastMessage = "";
        if (receiverList.size() == 0) {
            if (InputPatcher.patchProgram(textField.getText().toString(), smsSupplier.getProvider())) {
                toastMessage += "Patch successfully applied";
            } else {
                toastMessage += getString(R.string.text_noNumberInput);
            }
        }
        if (textField.getText().toString().trim().length() == 0) {
            toastMessage += (toastMessage.length() != 0) ? "\n" : "";
            toastMessage += getString(R.string.text_noTextInput);
        }
        if (toastMessage.length() > 0) {
            toast.setText(toastMessage);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }
        return true;
    }

    private void setRefreshButton() {
        ImageButton refreshButon = (ImageButton) findViewById(R.id.resfreshButton);
        final CharSequence progressText = getText(R.string.text_pleaseWait);
        refreshButon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressDialog.setMessage(progressText);
                progressDialog.show();
                if (SMSoIPApplication.getApp().getMinAPIVersion(smsSupplier) >= 14) {
                    new Thread(new RunnableFactory(SendActivity.this, progressDialog).getRefreshAndUpdateUIRunnable()).start();
                } else {
                    new Thread(new RunnableFactory(SendActivity.this, progressDialog).getRefreshInfosAndUpdateUIRunnable()).start();
                }
            }
        });

    }

    private void setClearButton() {
        ImageButton clearButton = (ImageButton) findViewById(R.id.clearButton);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getText(R.string.text_wantClear))
                .setCancelable(false)
                .setPositiveButton(getText(R.string.text_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        clearAllInputs();
                    }
                })
                .setNegativeButton(getText(R.string.text_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                alert.show();
            }
        });
    }


    private void clearAllInputs() {
        receiverList.clear();
        textField.setText("");
        updateViewOnChangedReceivers();
    }

    /**
     * updates the toast and the refresh informations after sending and/or refreshing
     *
     * @param resultMessage - the resultMessage to show in the Toast or null if only refresh was pressed
     * @param infoText      - the infoText on the screen or null if refresh was not successful
     * @param succesfulSent - was sending succesful
     * @deprecated will be removed in future releases
     */
    @Deprecated
    void showReturnMessage(CharSequence resultMessage, CharSequence infoText, boolean succesfulSent) {
        TextView infoView = (TextView) findViewById(R.id.infoText);
        if (infoText != null) {   //previous operation(s) was successful (send and/or refresh)
            infoView.setText(infoText);
        }
        if (resultMessage != null) {  //previous operation was a succesful refresh only, so no return message will be shown
            Spanned msg = new SpannableString(resultMessage);
            lastInfoDialog = new ImageDialog(this, succesfulSent, msg);
            lastInfoDialog.setOwnerActivity(this);
            lastInfoDialog.show();
            killDialogAfterAWhile(lastInfoDialog);
            if (succesfulSent) {
                writeSMSInDatabase(receiverList);
                clearAllInputs();
            } else {
                setInfoButtonVisibility();
            }
        }

    }

    private void killDialogAfterAWhile(final Dialog dialog) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                } finally {
                    dialog.dismiss();
                }
            }
        }).start();
    }

    private void writeSMSInDatabase(ArrayList<Receiver> receiverList) {
        boolean writeToDatabaseEnabled = settings.getBoolean(GlobalPreferences.GLOBAL_WRITE_TO_DATABASE, false) && SMSoIPApplication.getApp().isWriteToDatabaseAvailable();
        if (writeToDatabaseEnabled) {
            for (Receiver receiver : receiverList) {
                ContentValues values = new ContentValues();
                values.put("address", receiver.getReceiverNumber());
                String prefix = "";
                if (settings.getBoolean(GlobalPreferences.GLOBAL_ENABLE_PROVIDER_OUPUT, true)) {
                    prefix = "SMSoIP (" + smsSupplier.getProviderInfo() + "): ";
                }
                values.put("body", prefix + textField.getText().toString());
                getContentResolver().insert(Uri.parse("content://sms/sent"), values);
            }
        }
    }


    private void setTextArea() {
        textField = (EditText) findViewById(R.id.textInput);
        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (smsSupplier != null) { //activity was resumed
                    updateSMScounter(s);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                //do nothing
            }
        });
    }

    public void updateSMScounter(CharSequence charSequence) {
        int smsCount = 0;
        int messageLength = smsSupplier.getProvider().getTextMessageLength();

        if (charSequence.length() != 0) {
            smsCount = Math.round((charSequence.length() / messageLength));
            smsCount = charSequence.length() % messageLength == 0 ? smsCount : smsCount + 1;
        }
        smssigns.setText(String.format(SIGNSCONSTANT.toString(), charSequence.length(), smsCount));
    }

    private void setSearchButton() {
        searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT_REQUEST);
            }
        });

    }

    private void setSpinner() {
        spinner = (Spinner) findViewById(R.id.typeSpinner);
        smsSupplier.getProvider().createSpinner(this, spinner);
        findViewById(de.christl.smsoip.R.id.typeText).setVisibility(spinner.getVisibility());
    }

    /**
     * sending old style with only one result
     * will be removed in future releases
     *
     * @return
     */
    @Deprecated
    Result send() {
        List<Editable> numberList = new ArrayList<Editable>(receiverList.size());
        for (Receiver receiver : receiverList) {
            numberList.add(new SpannableStringBuilder(receiver.getReceiverNumber()));
        }
        return smsSupplier.fireSMS(textField.getText(), numberList, spinner.getVisibility() == View.INVISIBLE || spinner.getVisibility() == View.GONE ? null : spinner.getSelectedItem().toString());

    }

    /**
     * will be called for sending in a thread to update progress dialog
     * available since API Level 14
     *
     * @return
     */
    FireSMSResultList sendByThread() {
        return ((ExtendedSMSSupplier) smsSupplier).fireSMS(textField.getText().toString(), receiverList, spinner.getVisibility() == View.INVISIBLE || spinner.getVisibility() == View.GONE ? null : spinner.getSelectedItem().toString());
    }

    @Deprecated
    Result refreshInformations(boolean afterMessageSuccessfulSent) {
        return afterMessageSuccessfulSent ? smsSupplier.refreshInformationAfterMessageSuccessfulSent() : smsSupplier.refreshInformationOnRefreshButtonPressed();
    }

    /**
     * since API Level 14
     *
     * @param afterMessageSuccessfulSent
     * @return
     */
    SMSActionResult refreshInformationText(boolean afterMessageSuccessfulSent) {
        return afterMessageSuccessfulSent ? ((ExtendedSMSSupplier) smsSupplier).refreshInfoTextAfterMessageSuccessfulSent() : ((ExtendedSMSSupplier) smsSupplier).refreshInfoTextOnRefreshButtonPressed();
    }


    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri contactData = data.getData();
                final Receiver pickedReceiver = new DatabaseHandler(this).getPickedContactData(contactData);

                if (!pickedReceiver.getNumberTypeMap().isEmpty()) { //nothing picked or no number
                    //always one contact, so it will be filled always

                    final Map<String, String> numberTypeMap = pickedReceiver.getNumberTypeMap();
                    if (numberTypeMap.size() == 1) { //only one number, so choose this
                        addToReceiverList(pickedReceiver, (String) numberTypeMap.keySet().toArray()[0]);

                    } else { //more than one number for contact
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);

                        builder.setTitle(String.format(getText(R.string.text_pickNumber).toString(), pickedReceiver.getName()));
                        //build a map of string on screen with corresponding number for layout
                        final Map<String, String> presentationMap = new HashMap<String, String>();
                        for (Map.Entry<String, String> numberTypes : numberTypeMap.entrySet()) {
                            presentationMap.put(numberTypes.getKey() + " (" + numberTypes.getValue() + ")", numberTypes.getKey());
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
                                addToReceiverList(pickedReceiver, key);
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);


                    builder.setMessage(String.format(getText(R.string.text_noNumber).toString(), pickedReceiver.getName()))
                            .setCancelable(false)
                            .setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });


                    AlertDialog alert = builder.create();
                    alert.show();
                }

            }
        }

    }


    private void addToReceiverList(Receiver receiver, String receiverNumber) {
        int maxReceiverCount = smsSupplier.getProvider().getMaxReceiverCount();
        if (receiverList.size() < maxReceiverCount) {
            receiver.setReceiverNumber(receiverNumber);
            if (receiverList.addWithAlreadyInsertedCheck(receiver)) {
                toast.setText(R.string.text_receiver_added_twice);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
                toast.show();
            }
            updateViewOnChangedReceivers();
        } else {
            toast.setText(String.format(getText(R.string.text_max_receivers_reached).toString(), maxReceiverCount));
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
        }
    }


    private void updateViewOnChangedReceivers() {
        StringBuilder builder = new StringBuilder();
        //remove all disabled providers
        for (Iterator<Receiver> iterator = receiverList.iterator(); iterator.hasNext(); ) {
            Receiver next = iterator.next();
            if (!next.isEnabled()) {
                iterator.remove();
            }
        }
        for (int i = 0, receiverListSize = receiverList.size(); i < receiverListSize; i++) {
            Receiver receiver = receiverList.get(i);
            builder.append(receiver.getName());
            builder.append(i + 1 == receiverListSize ? "" : " ; ");
        }
        inputField.setText(builder.toString());
        //update the marking of textfield
        View viewById = findViewById(R.id.showChosenContacts);
        inputField.setOnClickListener(null);
        if (receiverList.size() > 0) {
            viewById.setVisibility(View.VISIBLE);
            inputField.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChosenContactsDialog();
                }
            });
        } else {
            viewById.setVisibility(View.GONE);
        }
        smsSupplier.getProvider().getMaxReceiverCount();
        if (receiverList.size() >= smsSupplier.getProvider().getMaxReceiverCount()) {
            addContactbyNumber.setVisibility(View.GONE);
            searchButton.setVisibility(View.GONE);
        } else {
            addContactbyNumber.setVisibility(View.VISIBLE);
            searchButton.setVisibility(View.VISIBLE);
        }
        setInfoButtonVisibility();

    }

    private void setInfoButtonVisibility() {
        View showInfoButton = findViewById(R.id.showInfoButton);
        if (lastInfoDialog != null) {
            showInfoButton.setVisibility(View.VISIBLE);
        } else {
            showInfoButton.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, PROVIDER_OPTION, 0, getString(R.string.text_provider_settings_short));
        item.setIcon(R.drawable.ic_menu_manage);
        MenuItem globalOption = menu.add(0, GLOBAL_OPTION, 0, getString(R.string.text_program_settings_short));
        globalOption.setIcon(R.drawable.ic_menu_compose);
        if (SMSoIPApplication.getApp().getProviderEntries().size() > 1) { //show only if more than one provider available
            item = menu.add(0, OPTION_SWITCH_SUPPLIER, 0, getString(R.string.text_changeProvider));
            item.setIcon(R.drawable.ic_menu_rotate);
        }

        item = menu.add(0, OPTION_SWITCH_ACCOUNT, 0, getString(R.string.text_changeAccount));
        item.setIcon(R.drawable.ic_menu_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PROVIDER_OPTION:
                startOptionActivity();
                return true;
            case OPTION_SWITCH_SUPPLIER:
                removeDialog(DIALOG_PROVIDER); //remove the chosenContactsDialog forces recreation
                showDialog(DIALOG_PROVIDER);
                return true;
            case GLOBAL_OPTION:
                startGlobalOptionActivity();
                return true;
            case OPTION_SWITCH_ACCOUNT:
                removeDialog(DIALOG_SWITCH_ACCOUNT); //remove the chosenContactsDialog forces recreation
                showDialog(DIALOG_SWITCH_ACCOUNT);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startGlobalOptionActivity() {
        Intent pref = new Intent(this, GlobalPreferences.class);
        startActivity(pref);
        optionsCalled = true;
    }


    private void startOptionActivity() {
        Intent intent = new Intent(this, ProviderPreferences.class);
        intent.putExtra(ProviderPreferences.SUPPLIER_CLASS_NAME, smsSupplier.getClass().getCanonicalName());
        startActivity(intent);
        optionsCalled = true;
    }


    @Override
    protected void onStop() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
            progressDialog = null;
        }
        super.onStop();
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        Dialog dialog = null;
        switch (id) {
            case DIALOG_SMILEYS:
                final CharSequence[] smileyItems = {";)", ":-)", ":-))", ":-(", ":-((", ";-)", ":-D", ":-@", ":-O", ":-|", ":-o", ":~-(", ":-*", ":-#", ":-s", "(^_^)", "(^_~)", "d(^_^)b", "(+_+)", "(>_<)", "(-_-)", "=^.^="};

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(smileyItems, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        CharSequence smiley = smileyItems[item];
                        dialog.dismiss();
                        final int selStart = textField.getSelectionStart();
                        final int selEnd = textField.getSelectionEnd();
                        StringBuffer result = new StringBuffer(textField.getText().subSequence(0, selStart));
                        result.append(" ").append(smiley).append(" ").append(textField.getText().subSequence(selEnd, textField.length()));
                        textField.setText(result);

                    }
                });
                dialog = builder.create();
                break;
            case DIALOG_PROVIDER:
                Map<String, ProviderEntry> providerEntries = SMSoIPApplication.getApp().getProviderEntries();
                if (providerEntries.size() == 0) { //skip if no provider available
                    break;
                }
                final List<ProviderEntry> filteredProviderEntries = new ArrayList<ProviderEntry>();
                if (smsSupplier == null) {   //add all if current provider not set
                    filteredProviderEntries.addAll(providerEntries.values());
                } else {
                    for (ProviderEntry providerEntry : providerEntries.values()) {     //filter out cause current provider should not be shown
                        if (!providerEntry.getSupplierClassName().equals(smsSupplier.getClass().getCanonicalName())) {
                            filteredProviderEntries.add(providerEntry);
                        }
                    }
                }
                int filteredProvidersSize = filteredProviderEntries.size();
                if (filteredProvidersSize > 1) {
                    final CharSequence[] providerItems = new String[filteredProvidersSize];
                    for (int i = 0; i < filteredProvidersSize; i++) {
                        ProviderEntry providerEntry = filteredProviderEntries.get(i);
                        providerItems[i] = providerEntry.getProviderName();
                    }
                    builder = new AlertDialog.Builder(this);
                    builder.setItems(providerItems, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String supplierClassName = filteredProviderEntries.get(item).getSupplierClassName();
                            dialog.dismiss();
                            changeSupplier(supplierClassName);
                        }
                    });
                    builder.setTitle(R.string.text_chooseProvider);
                    builder.setCancelable(providerEntries.size() != filteredProvidersSize); //only cancelable on switch providers
                    dialog = builder.create();
                } else {  //have to be a min of 1 here, else button will not be available
                    changeSupplier(filteredProviderEntries.get(0).getSupplierClassName());
                }
                break;
            case DIALOG_NUMBER_INPUT:
                final EditText input = new EditText(this);
                builder = new AlertDialog.Builder(this) {

                    @Override
                    public AlertDialog create() {
                        AlertDialog dialog = super.create();
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        return dialog;
                    }
                };
                builder.setMessage(getText(R.string.text_add_contact_by_number_dialog));

                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setSingleLine();
                builder.setView(input);
                builder.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String rawNumber = input.getText().toString();
                        if (!rawNumber.equals("")) {
                            DatabaseHandler dbHandler = new DatabaseHandler(SendActivity.this);
                            Receiver contactByNumber = dbHandler.findContactByNumber(rawNumber);
                            if (contactByNumber == null) {
                                contactByNumber = new Receiver("-1", getText(R.string.text_unknown).toString(), 0);
                                contactByNumber.addNumber(rawNumber, getText(R.string.text_unknown).toString());
                            }
                            String number = contactByNumber.getFixedNumberByRawNumber(rawNumber);
                            addToReceiverList(contactByNumber, number);
                        }
                        input.setText("");
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
            case DIALOG_SWITCH_ACCOUNT:
                OptionProvider provider = smsSupplier.getProvider();
                Map<Integer, AccountModel> accounts = provider.getAccounts();
                Map<Integer, AccountModel> filteredAccounts = new HashMap<Integer, AccountModel>();
                Integer currentAccount = provider.getCurrentAccountIndex();
                //filter list by current
                for (Integer integer : accounts.keySet()) {
                    if (integer.equals(currentAccount)) {
                        continue;
                    }
                    filteredAccounts.put(integer, accounts.get(integer));
                }
                switch (filteredAccounts.size()) {
                    case 1:
                        for (Integer accountId : filteredAccounts.keySet()) {
                            switchAccount(accountId);
                            break;
                        }
                        break;
                    default:
                        builder = new AlertDialog.Builder(this);
                        CharSequence[] items = new CharSequence[filteredAccounts.size()];
                        int i = 0;
                        final Map<Integer, Integer> charAccountRel = new HashMap<Integer, Integer>(filteredAccounts.size());
                        for (Integer index : filteredAccounts.keySet()) {
                            items[i] = filteredAccounts.get(index).getUserName();
                            charAccountRel.put(i++, index);
                        }
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                dialog.dismiss();
                                switchAccount(charAccountRel.get(item));
                            }
                        });
                        builder.setTitle(R.string.text_chooseAccount);
                        dialog = builder.create();
                        break;
                }

                break;
            default:
                dialog = super.onCreateDialog(id);
        }
        lastDialog = dialog;
        return dialog;
    }

    private void switchAccount(Integer accountId) {
        smsSupplier.getProvider().setCurrentAccountId(accountId);
        setFullTitle();
        updateInfoTextSilent();
    }

    private void changeSupplier(String supplierClassName) {
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName);
        setFullTitle();
        //reset all not needed informations
        updateSMScounter();
        setSpinner();
        updateAfterReceiverCountChanged();
        updateInfoTextSilent();
    }

    public void updateAfterReceiverCountChanged() {
        int maxReceiverCount = smsSupplier.getProvider().getMaxReceiverCount();

        if (receiverList.size() > maxReceiverCount) {
            CheckForDuplicatesArrayList newReceiverList = new CheckForDuplicatesArrayList();
            for (int i = 0; i < maxReceiverCount; i++) {
                newReceiverList.add(receiverList.get(i));

            }
            receiverList = newReceiverList;
            updateViewOnChangedReceivers();
            toast.setText(String.format(getText(R.string.text_too_much_receivers).toString(), maxReceiverCount));
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
        }
        updateViewOnChangedReceivers();
    }


    public void updateSMScounter() {
        updateSMScounter(textField.getText());
    }

    @Override
    public boolean onSearchRequested() {
        if (searchButton.getVisibility() == View.VISIBLE) {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT_REQUEST);
        }
        return false;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //close open dialog if any
        if (lastDialog != null) {
            lastDialog.dismiss();
        }
        if (smsSupplier != null) { //only save instance if provider is already chosen
            outState.putString(SAVED_INSTANCE_SUPPLIER, smsSupplier.getClass().getCanonicalName());
            outState.putCharSequence(SAVED_INSTANCE_INPUTFIELD, inputField.getText());
            outState.putParcelableArrayList(SAVED_INSTANCE_RECEIVERS, receiverList);
            CharSequence infoText = ((TextView) findViewById(R.id.infoText)).getText();
            outState.putCharSequence(SAVED_INSTANCE_INFO, infoText);
            Integer currentAccountIndex = smsSupplier.getProvider().getCurrentAccountIndex();
            if (currentAccountIndex != null) {
                outState.putInt(SAVED_INSTANCE_ACCOUNT_ID, currentAccountIndex);
            }
            if (spinner.getVisibility() == View.VISIBLE) {
                outState.putInt(SAVED_INSTANCE_SPINNER, spinner.getSelectedItemPosition());
            }
        }
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        MenuItem item = menu.findItem(OPTION_SWITCH_ACCOUNT);
        //sho only if more than one account is available
        item.setVisible(smsSupplier.getProvider().getAccounts().size() > 1);
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * since API level 14
     *
     * @param fireSMSResults
     * @param infoText
     */
    public void showReturnMessage(FireSMSResultList fireSMSResults, String infoText) {
        TextView infoView = (TextView) findViewById(R.id.infoText);
        if (infoText != null) {   //previous operation(s) was successful (send and/or refresh)
            infoView.setText(infoText);
        }
        StringBuilder resultMessage = new StringBuilder();
        if (fireSMSResults.size() == 1) {  // nobody cares about extra Infos if only one message was sent
            resultMessage.append(fireSMSResults.get(0).getResult().getMessage());
        } else {
            String unknownReceiverText = getText(R.string.text_unknown).toString();
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
        lastInfoDialog = new EmoImageDialog(this, fireSMSResults.getResult(), resultMessage.toString());
        lastInfoDialog.setOwnerActivity(this);
        lastInfoDialog.show();
        killDialogAfterAWhile(lastInfoDialog);
        writeSMSInDatabase(fireSMSResults.getSuccessList());
        if (fireSMSResults.getResult() == FireSMSResultList.SendResult.SUCCESS) {
            clearAllInputs();
        } else {
            receiverList.removeAll(fireSMSResults.getSuccessList());
            updateViewOnChangedReceivers();
        }
    }
}
