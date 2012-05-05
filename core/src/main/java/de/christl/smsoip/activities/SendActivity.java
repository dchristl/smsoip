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
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.*;
import android.text.method.DigitsKeyListener;
import android.view.*;
import android.widget.*;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.provider.SMSSupplier;
import de.christl.smsoip.ui.ImageDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SendActivity extends DefaultActivity {

    private EditText inputField, textField;
    TextView smssigns;
    private Spinner spinner;

    private static final int PICK_CONTACT_REQUEST = 0;

    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    public static final String GIVEN_NUMBER = "givenNumber";
    public CharSequence SIGNSCONSTANT;

    private ProgressDialog progressDialog;


    public static String infoMsg = null;
    final Handler updateUIHandler = new Handler();


    final Runnable updateRunnable = new Runnable() {
        public void run() {
            showReturnMessage();
        }
    };
    public Toast toast;
    private SMSSupplier smsSupplier;
    private static final int PROVIDER_OPTION = 30;
    private static final int OPTION_SWITCH = 31;
    private static final int DIALOG_SMILEYS = 32;
    private static final int DIALOG_PROVIDER = 33;
    private static final int GLOBAL_OPTION = 34;

    private CharSequence infoText;
    private Result result;
    private SharedPreferences settings;


    @Override
    protected void onResume() {
        super.onResume();
        smsSupplier.getProvider().refresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.sendactivity);
        SIGNSCONSTANT = getText(R.string.text_smssigns);
        if (infoMsg != null) {
            ((TextView) findViewById(R.id.infoText)).setText(infoMsg);
        }
        inputField = (EditText) findViewById(R.id.numberInput);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        smssigns = (TextView) findViewById(R.id.smssigns);
        smssigns.setText(String.format(SIGNSCONSTANT.toString(), 0, 0));
        toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        Bundle currProvider = this.getIntent().getExtras();
        smsSupplier = SMSoIPApplication.getApp().getInstance((String) currProvider.get(SUPPLIER_CLASS_NAME));
        setTitle(smsSupplier.getProvider().getProviderName());
        setSpinner();

        Button sendButton = (Button) findViewById(R.id.sendButton);
        final CharSequence progressText = getText(R.string.text_smscomitted);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!precheck() || progressDialog == null) {
                    return;
                }
                progressDialog.setMessage(progressText);
                progressDialog.show();
                new Thread(new Runnable() {
                    public void run() {
                        send();
                        updateUIHandler.post(updateRunnable);
                        progressDialog.cancel();
                    }


                }).start();

            }
        });

        if (currProvider.get(GIVEN_NUMBER) != null && !String.valueOf(currProvider.get(GIVEN_NUMBER)).equals("")) {
            setReceiverNumber(String.valueOf(currProvider.get(GIVEN_NUMBER)));
        }
        setNumberListener();
        setSearchButton();
        setClearButton();
        setRefreshButton();
        setSigButton();
        setShortTextButton();
        setSmileyButton();
        setTextArea();
        insertAds((LinearLayout) findViewById(R.id.linearLayout), this);
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

    private void setNumberListener() {
        DigitsKeyListener numberListener =
                new DigitsKeyListener(false, false); // first true : is signed, second one : is decimal
        inputField.setKeyListener(numberListener);


    }

    private boolean precheck() {
        String toastMessage = "";
        if (inputField.getText().toString().trim().length() == 0) {
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
                new Thread(new Runnable() {
                    public void run() {
                        refreshInformations(false);
                        updateUIHandler.post(updateRunnable);
                        progressDialog.cancel();
                    }
                }).start();
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
        inputField.setText("");
        textField.setText("");
    }

    private void showReturnMessage() {
        TextView infoView = (TextView) findViewById(R.id.infoText);
        if (result == null) {  //break if only refresh is pressed and all is valid
            infoView.setText(infoText);
            return;
        }
        if (result.equals(Result.NO_ERROR)) {
            infoView.setText(infoText);
            Spanned msg = new SpannableString(getText(R.string.text_smsSend_Success));
            final ImageDialog dialog = new ImageDialog(this, true, msg);
            dialog.setOwnerActivity(this);
            dialog.show();
            killDialogAfterAWhile(dialog);
            writeSMSInDatabase();
            clearAllInputs();
        } else {
            Spanned msg = new SpannableString(result.getUserText());
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
        ContentValues values = new ContentValues();
        values.put("address", inputField.getText().toString());
        String prefix = "";
        if (settings.getBoolean(GlobalPreferences.GLOBAL_ENABLE_PROVIDER_OUPUT, true)) {
            prefix = "SMSoIP (" + smsSupplier.getProviderInfo() + "): ";
        }
        values.put("body", prefix + textField.getText().toString());
        getContentResolver().insert(Uri.parse("content://sms/sent"), values);
    }


    private void setTextArea() {
        textField = (EditText) findViewById(R.id.textInput);
        textField.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int smsCount = 0;
                if (charSequence.length() != 0) {
                    smsCount = Math.round((charSequence.length() / 160));
                    smsCount = charSequence.length() % 160 == 0 ? smsCount : smsCount + 1;
                }
                smssigns.setText(String.format(SIGNSCONSTANT.toString(), charSequence.length(), smsCount));
            }

            public void afterTextChanged(Editable editable) {

            }
        });

    }

    private void setSearchButton() {
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT_REQUEST);
            }
        });

    }

    private void setSpinner() {
        String[] array_spinner = smsSupplier.getSpinnerItems();

        boolean isVisible = array_spinner != null && array_spinner.length > 0;
        int viewType = isVisible ? View.VISIBLE : View.INVISIBLE;
        findViewById(R.id.typeText).setVisibility(viewType);
        spinner = (Spinner) findViewById(R.id.typeSpinner);
        spinner.setVisibility(viewType);
        if (isVisible) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, array_spinner);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
        }

    }

    private void send() {
        List<Editable> receiverList = new ArrayList<Editable>();
        receiverList.add(inputField.getText());
        result = smsSupplier.fireSMS(textField.getText(), receiverList, spinner.getVisibility() == View.INVISIBLE ? null : spinner.getSelectedItem().toString());
        if (result.equals(Result.NO_ERROR)) {
            refreshInformations(true);
        }
    }

    private void refreshInformations(boolean afterMessageSuccessfulSent) {
        Result tmpResult = afterMessageSuccessfulSent ? smsSupplier.refreshInformationAfterMessageSuccessfulSent() : smsSupplier.refreshInformationOnRefreshButtonPressed();
        infoText = tmpResult.getUserText();
        if (!tmpResult.equals(Result.NO_ERROR)) {
            result = tmpResult;
        } else if (!afterMessageSuccessfulSent) {
            result = null; //set it to null if only refresh button is pressed, return is not from interest
        }
    }


    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == PICK_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                String pickedId = null;
                boolean hasPhone = false;
                Uri contactData = data.getData();
                Cursor contactCur = managedQuery(contactData, null, null, null, null);
                if (contactCur.moveToFirst()) {
                    pickedId = contactCur.getString(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
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
                        String description = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(this.getResources(), currEntry.getValue(), "other");
                        presentationLayer.put(currEntry.getKey(), currEntry.getKey() + " (" + description + ")");
                    }
                    if (presentationLayer.size() == 1) {
                        for (String s : presentationLayer.keySet()) {
                            setReceiverNumber(s);
                        }
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    final String[] items = presentationLayer.values().toArray(new String[presentationLayer.size()]);
                    builder.setTitle(getText(R.string.text_pickNumber));
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            String key = null;
                            for (Map.Entry<String, String> entry : presentationLayer.entrySet()) {
                                if (entry.getValue().equals(items[item])) {
                                    key = entry.getKey();
                                    break;
                                }
                            }
                            setReceiverNumber(presentationLayer.get(key));
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


    private void setReceiverNumber(String rawNumber) {
        String prefix = "";
        if (!rawNumber.startsWith("+") && !rawNumber.startsWith("00")) {
            String areaCode = settings.getString(GlobalPreferences.GLOBAL_AREA_CODE, "49");
            prefix = "00" + areaCode;
        }
        rawNumber = rawNumber.replaceFirst("^0", "");
        rawNumber = rawNumber.replaceFirst("\\+", "00");
        rawNumber = rawNumber.replaceAll("[^0-9]", "");
        inputField.setText(prefix + rawNumber);
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
        infoMsg = ((TextView) findViewById(R.id.infoText)).getText().toString();
        super.onStop();
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        Dialog dialog = null;
        switch (id) {
            case DIALOG_SMILEYS:
                final CharSequence[] smileyItems = {";)", ":-)", ":-))", ":-(", ":-((", ";-)", ":-D", ":-@", ":-O", ":-|", ":-o", ":~-(", ":-*", ":-#", ":-s", "(^_^)", "(^_~)", "d(^_^)b", "(+_+)", "(>_<)", "(-_-)", "=^.^="};

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setIcon(de.christl.smsoip.R.drawable.checkloginbutton);
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
            default:
                dialog = super.onCreateDialog(id);
        }
        return dialog;
    }

    private void changeSupplier(String supplierClassName) {
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName);
        setTitle(smsSupplier.getProvider().getProviderName());
        //reset all not needed infoormations
        ((TextView) findViewById(R.id.infoText)).setText(R.string.text_notyetrefreshed);
        infoText = null;
        result = null;
        setSpinner();
    }


}
