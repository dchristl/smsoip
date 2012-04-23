package de.christl.smsoip.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

public class OptionActivity extends DefaultActivity {

    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    public static final String LASTACTIVITY = "lastActivity";
    private EditText userNameInput;
    private EditText passwordInput;
    private EditText signatureInput;
    private OptionProvider provider;

    private Class lastActivity = MainActivity.class;
    private ProgressDialog progressDialog;
    final Handler updateUIHandler = new Handler();
    final Runnable updateRunnable = new Runnable() {
        public void run() {
            updateButtons();
        }
    };
    private Result result;
    private SMSSupplier smsSupplier;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionactivity);
        userNameInput = (EditText) findViewById(R.id.userNameInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);
        signatureInput = (EditText) findViewById(R.id.signatureInput);
        //fix for different size of PW and username (no sense really, but works ;) )
        passwordInput.setHeight(userNameInput.getHeight());

        Bundle extras = getIntent().getExtras();
        String supplierClassName = (String) extras.get(SUPPLIER_CLASS_NAME);

        if (extras.get(LASTACTIVITY) != null) {
            lastActivity = SendActivity.class;
        }
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName);
        provider = smsSupplier.getProvider();
        setValues();
        setTitle(getString(R.string.text_optionTitle) + " " + provider.getProviderName());
        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                saveSettings();
            }
        });
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setMessage(getString(R.string.text_checkCredentials));
        ImageButton checkLoginButton = (ImageButton) findViewById(R.id.checkLoginButton);
        checkLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkLogin();
                        progressDialog.cancel();
                        updateUIHandler.post(updateRunnable);
                    }
                }).start();

            }
        });
        insertAds((LinearLayout) findViewById(R.id.linearLayout), this);
    }

    private void checkLogin() {
        result = smsSupplier.login(userNameInput.getText().toString(), passwordInput.getText().toString());
    }

    private void updateButtons() {
        int color = result.equals(Result.NO_ERROR) ? Color.GREEN : Color.RED;
        userNameInput.setTextColor(color);
        passwordInput.setTextColor(color);
    }


    private void saveSettings() {
        provider.setPassword(passwordInput.getText().toString());
        provider.setUserName(userNameInput.getText().toString());
        provider.setSignature(signatureInput.getText().toString());
        provider.save();
        startLastActivity();
    }

    private void startLastActivity() {
        Intent intent = new Intent(this, lastActivity);
        if (lastActivity.equals(MainActivity.class)) {
            intent.putExtra(MainActivity.PARAMETER, true);
        } else {
            intent.putExtra(SendActivity.SUPPLIER_CLASS_NAME, (String) getIntent().getExtras().get(SUPPLIER_CLASS_NAME));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    private boolean setValues() {
        userNameInput.setText(provider.getUserName());
        passwordInput.setText(provider.getPassword());
        signatureInput.setText(provider.getSignature());
        return true;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            handleBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void handleBackPressed() {
        if (!settingsChanged()) {
            startLastActivity();
            return;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    saveSettings();
                } else {
                    startLastActivity();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.text_saveChanges)).setPositiveButton(getString(R.string.text_yesOption), dialogClickListener)
                .setNegativeButton(getString(R.string.text_noOption), dialogClickListener).show();
    }

    private boolean settingsChanged() {
        if (!userNameInput.getText().toString().equals(provider.getUserName())) {
            return true;
        } else if (!passwordInput.getText().toString().equals(provider.getPassword())) {
            return true;
        } else if (!signatureInput.getText().toString().equals(provider.getSignature())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (settingsChanged()) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        saveAndHandleMenu(item);
                    } else {
                        handleMenu(item);
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.text_saveChanges)).setPositiveButton(getString(R.string.text_yesOption), dialogClickListener)
                    .setNegativeButton(getString(R.string.text_noOption), dialogClickListener).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    private void saveAndHandleMenu(MenuItem item) {
        provider.setPassword(passwordInput.getText().toString());
        provider.setUserName(userNameInput.getText().toString());
        provider.setSignature(signatureInput.getText().toString());
        provider.save();
        handleMenu(item);
    }

    private void handleMenu(MenuItem item) {
        super.onOptionsItemSelected(item);
    }


}

