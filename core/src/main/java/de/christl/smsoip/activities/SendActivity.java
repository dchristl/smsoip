package de.christl.smsoip.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.*;
import android.view.*;
import android.widget.*;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.provider.SMSSupplier;
import de.christl.smsoip.ui.ChosenContactsDialog;
import de.christl.smsoip.ui.ImageDialog;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SendActivity extends DefaultActivity {

    private EditText inputField, textField;
    TextView smssigns;
    private Spinner spinner;

    private static final int PICK_CONTACT_REQUEST = 0;

    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    public static final String GIVEN_NUMBER = "givenNumber";
    public static final String GIVEN_NAME = "givenName";
    public static String GIVEN_ID = "givenId";


    public CharSequence SIGNSCONSTANT;
    private ProgressDialog progressDialog;


    public Toast toast;
    private SMSSupplier smsSupplier;
    private static final int PROVIDER_OPTION = 30;
    private static final int OPTION_SWITCH = 31;
    private static final int DIALOG_SMILEYS = 32;
    private static final int DIALOG_PROVIDER = 33;

    private static final int GLOBAL_OPTION = 34;
    private static final int DIALOG_NUMBER_INPUT = 35;
    private SharedPreferences settings;
    List<Receiver> receiverList = new ArrayList<Receiver>();
    private View addContactbyNumber;
    private ImageButton searchButton;

    @Override
    protected void onResume() {
        super.onResume();
//        refresh all settings, cause it can be changed
        smsSupplier.getProvider().refresh();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
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
        Bundle currProvider = this.getIntent().getExtras();
        smsSupplier = SMSoIPApplication.getApp().getInstance((String) currProvider.get(SUPPLIER_CLASS_NAME), this);
        setTitle(smsSupplier.getProvider().getProviderName());
        setSpinner();

        Button sendButton = (Button) findViewById(R.id.sendButton);
        final CharSequence progressText = getText(R.string.text_smscomitted);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!preSendCheck() || progressDialog == null) {
                    return;
                }
                progressDialog.setMessage(progressText);
                progressDialog.show();
                new Thread(new RunnableFactory(SendActivity.this, progressDialog).getSendAndUpdateUIRunnable()).start();

            }
        });

        if (currProvider.get(GIVEN_NUMBER) != null && !String.valueOf(currProvider.get(GIVEN_NUMBER)).equals("")) {
            addToReceiverList(((String) currProvider.get(GIVEN_ID)), ((String) currProvider.get(GIVEN_NAME)), ((String) currProvider.get(GIVEN_NUMBER)));
        }
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
        setContactsByNumberInput();
        insertAds((LinearLayout) findViewById(R.id.linearLayout), this);
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
        final ChosenContactsDialog dialog = new ChosenContactsDialog(this, receiverList);
        dialog.setOwnerActivity(this);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateViewOnChangedReceivers();
            }
        });
        dialog.show();
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
            toastMessage += getString(R.string.text_noNumberInput);
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
                new Thread(new RunnableFactory(SendActivity.this, progressDialog).getRefreshInfosAndUpdateUIRunnable()).start();
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
                .setNegativeButton(getText(R.string.text_no), new DialogInterface.OnClickListener() {
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
     */
    void showReturnMessage(CharSequence resultMessage, CharSequence infoText) {
        TextView infoView = (TextView) findViewById(R.id.infoText);
        if (infoText != null) {   //previous operation(s) was successful (send and/or refresh)
            infoView.setText(infoText);
            if (resultMessage != null) {  //previous operation was a refresh only, so no return message will be shown
                Spanned msg = new SpannableString(resultMessage);
                final ImageDialog dialog = new ImageDialog(this, true, msg);
                dialog.setOwnerActivity(this);
                dialog.show();
                killDialogAfterAWhile(dialog);
                if (settings.getBoolean(GlobalPreferences.GLOBAL_WRITE_TO_DATABASE, false) && SMSoIPApplication.getApp().isWriteToDatabaseAvailable()) {
                    writeSMSInDatabase();
                }
                clearAllInputs();
            }
        } else {
            Spanned msg = new SpannableString(resultMessage);
            final ImageDialog dialog = new ImageDialog(this, false, msg);
            dialog.setOwnerActivity(this);
            dialog.show();
            killDialogAfterAWhile(dialog);
        }
    }

    private void killDialogAfterAWhile(final ImageDialog dialog) {
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

    private void writeSMSInDatabase() {
        for (Receiver receiver : receiverList) {
            ContentValues values = new ContentValues();
            values.put("address", receiver.getRawNumber());
            String prefix = "";
            if (settings.getBoolean(GlobalPreferences.GLOBAL_ENABLE_PROVIDER_OUPUT, true)) {
                prefix = "SMSoIP (" + smsSupplier.getProviderInfo() + "): ";
            }
            values.put("body", prefix + textField.getText().toString());
            getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }
    }


    private void setTextArea() {
        textField = (EditText) findViewById(R.id.textInput);
        textField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateSMScounter(charSequence);
            }

            public void afterTextChanged(Editable editable) {

            }
        });

    }

    private void updateSMScounter(CharSequence charSequence) {
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

    Result send() {
        List<Editable> numberList = new ArrayList<Editable>(receiverList.size());
        for (Receiver receiver : receiverList) {
            numberList.add(new SpannableStringBuilder(receiver.getReceiverNumber()));
        }
        return smsSupplier.fireSMS(textField.getText(), numberList, spinner.getVisibility() == View.INVISIBLE || spinner.getVisibility() == View.GONE ? null : spinner.getSelectedItem().toString());

    }

    Result refreshInformations(boolean afterMessageSuccessfulSent) {
        return afterMessageSuccessfulSent ? smsSupplier.refreshInformationAfterMessageSuccessfulSent() : smsSupplier.refreshInformationOnRefreshButtonPressed();
    }


    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                String pickedId = null;
                boolean hasPhone = false;
                String name = null;
                Uri contactData = data.getData();
                Cursor contactCur = managedQuery(contactData, null, null, null, null);
                if (contactCur.moveToFirst()) {
                    pickedId = contactCur.getString(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                    name = contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    hasPhone = Integer.parseInt(contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
                }
                if (pickedId != null && hasPhone) {
                    Cursor phones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{pickedId}, null);
                    HashMap<String, Integer> phoneNumber = new HashMap<String, Integer>();
                    while (phones.moveToNext()) {
                        phoneNumber.put(phones.getString(
                                phones.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER)), phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2)));
                    }
                    phones.close();
                    final HashMap<String, String> presentationLayer = new HashMap<String, String>();
                    for (Map.Entry<String, Integer> currEntry : phoneNumber.entrySet()) {
                        String description = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.getResources(), currEntry.getValue(), getText(R.string.text_no_phone_type_label));
                        presentationLayer.put(currEntry.getKey(), currEntry.getKey() + " (" + description + ")");
                    }
                    if (presentationLayer.size() == 1) {
                        for (String s : presentationLayer.keySet()) {
                            addToReceiverList(pickedId, name, s);
                        }
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final String[] items = presentationLayer.values().toArray(new String[presentationLayer.size()]);
                    builder.setTitle(getText(R.string.text_pickNumber));
                    final String finalName = name;
                    final String finalPickedId = pickedId;
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String key = null;
                            for (Map.Entry<String, String> entry : presentationLayer.entrySet()) {
                                if (entry.getValue().equals(items[item])) {
                                    key = entry.getKey();
                                    break;
                                }
                            }
                            addToReceiverList(finalPickedId, finalName, key);
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.text_noNumber)
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

    private void addToReceiverList(String pickedId, String name, String receiverNumber) {
        int maxReceiverCount = smsSupplier.getProvider().getMaxReceiverCount();
        if (receiverList.size() < maxReceiverCount) {
            receiverList.add(new Receiver(pickedId, name, receiverNumber));
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
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && !isDefaultSet()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    boolean isDefaultSet() {
        return !settings.getString(GlobalPreferences.GLOBAL_DEFAULT_PROVIDER, "").equals("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, PROVIDER_OPTION, 0, getString(R.string.text_provider_settings_short));
        item.setIcon(R.drawable.settingsbutton);
        MenuItem globalOption = menu.add(0, GLOBAL_OPTION, 0, getString(R.string.text_program_settings_short));
        globalOption.setIcon(R.drawable.settingsbutton);
        if (SMSoIPApplication.getApp().getProviderEntries().size() > 1) { //show only if more than one provider available
            item = menu.add(0, OPTION_SWITCH, 0, getString(R.string.text_changeProvider));
            item.setIcon(R.drawable.changeprovider);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PROVIDER_OPTION:
                startOptionActivity();
                return true;
            case OPTION_SWITCH:
                removeDialog(DIALOG_PROVIDER); //remove the dialog forces recreation
                showDialog(DIALOG_PROVIDER);
                return true;
            case GLOBAL_OPTION:
                Intent pref = new Intent(this, GlobalPreferences.class);
                startActivity(pref);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void startOptionActivity() {
        Intent intent =
                new Intent(this, ProviderPreferences.class);
        intent.putExtra(ProviderPreferences.SUPPLIER_CLASS_NAME, smsSupplier.getClass().getCanonicalName());
        startActivity(intent);
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
                final List<ProviderEntry> filteredProviderEntries = new ArrayList<ProviderEntry>();
                for (ProviderEntry providerEntry : providerEntries.values()) {     //filter out cause current provider should not be shown
                    if (!providerEntry.getSupplierClassName().equals(smsSupplier.getClass().getCanonicalName())) {
                        filteredProviderEntries.add(providerEntry);
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
                            addToReceiverList("-1", (String) getText(R.string.text_unknown), rawNumber);
                        }
                        input.setText("");
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
            default:
                dialog = super.onCreateDialog(id);
        }
        return dialog;
    }

    private void changeSupplier(String supplierClassName) {
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName, this);
        setTitle(smsSupplier.getProvider().getProviderName());
        //reset all not needed informations
        ((TextView) findViewById(R.id.infoText)).setText(R.string.text_notyetrefreshed);
        updateSMScounter();
        setSpinner();
        int maxReceiverCount = smsSupplier.getProvider().getMaxReceiverCount();

        if (receiverList.size() > maxReceiverCount) {
            List<Receiver> newReceiverList = new ArrayList<Receiver>();
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
}
