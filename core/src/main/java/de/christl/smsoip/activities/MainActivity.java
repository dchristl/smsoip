package de.christl.smsoip.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.Map;

public class MainActivity extends AllActivity {

    private final int PROVIDER_OPTION = 20;
    private final int GLOBAL_OPTION = 21;
    private Spinner spinner;
    public static final String PARAMETER = "nonskip";
    private String givenNumber;
    private Map<String, ProviderEntry> providerEntries;
    private SharedPreferences settings;
    private String name;
    private String id = "-1";


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        Uri data = getIntent().getData();
        if (data != null) {
            name = (String) getText(R.string.text_unknown);
            givenNumber = data.getSchemeSpecificPart();
            String[] projection = new String[]{
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup._ID};
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(givenNumber));
            Cursor query = getContentResolver().query(uri, projection, null, null, null);
            while (query.moveToNext()) {
                name = query.getString(query.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                if (name == null || name.equals("")) {
                    name = (String) getText(R.string.text_unknown);
                }
                id = query.getString(query.getColumnIndex(ContactsContract.PhoneLookup._ID));
                if (id == null || id.equals("")) {
                    id = "-1";
                }
            }
            query.close();
        } else {
            givenNumber = null;
        }

        providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        setContentView(R.layout.main);
        Bundle extras = getIntent().getExtras();
        String defaultSupplierClass = getDefaultSupplier();
        Boolean nonskip = extras == null ? null : (Boolean) extras.get(PARAMETER);

        int providersSize = providerEntries.size();
        if (providersSize == 0) {
            return;
        }
        if (providersSize == 1) {
            nonskip = nonskip == null ? false : nonskip;
            for (ProviderEntry providerEntry : providerEntries.values()) {
                defaultSupplierClass = providerEntry.getSupplierClassName();
                saveDefaultProvider(defaultSupplierClass);
            }
        }
        addSpinner(defaultSupplierClass, nonskip);
        addNextButton();
        insertAds((LinearLayout) findViewById(R.id.linearLayout), this);
    }

    private String getDefaultSupplier() {
        String string = settings.getString(GlobalPreferences.GLOBAL_DEFAULT_PROVIDER, "");
        //check if default provider is installed
        if (!string.equals("")) {
            boolean found = false;
            for (ProviderEntry providerEntry : providerEntries.values()) {
                if (providerEntry.getSupplierClassName().equals(string)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                string = "";
                saveDefaultProvider(string);
            }
        }

        return string;
    }

    private void addSpinner(String defaultSupplierClass, Boolean nonskip) {

        final ProviderEntry[] arrayAdapter = providerEntries.values().toArray(new ProviderEntry[providerEntries.size()]);
        ArrayAdapter<ProviderEntry> adapter = new ArrayAdapter<ProviderEntry>(this,
                android.R.layout.simple_spinner_item, arrayAdapter);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = (Spinner) findViewById(R.id.providerSpinner);
        spinner.setAdapter(adapter);
        if ((nonskip == null) || !nonskip) {
            if (!defaultSupplierClass.equals("")) {
                Intent intent = new Intent(MainActivity.this, SendActivity.class);
                intent.putExtra(SendActivity.SUPPLIER_CLASS_NAME, defaultSupplierClass);
                intent.putExtra(SendActivity.GIVEN_NUMBER, givenNumber);
                intent.putExtra(SendActivity.GIVEN_NAME, name);
                intent.putExtra(SendActivity.GIVEN_ID, id);
                SendActivity.infoMsg = null;
                startActivity(intent);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selection = arrayAdapter[i].getSupplierClassName();
                String defaultSupplierClassName = readOutDefaultSupplier();
                boolean checked = defaultSupplierClassName != null && defaultSupplierClassName.equals(selection);
                ((CheckBox) findViewById(R.id.defaultCheckBox)).setChecked(checked);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void addNextButton() {
        Button next = (Button) findViewById(R.id.nextButton);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CheckBox cb = (CheckBox) findViewById(R.id.defaultCheckBox);
                String defaultSupplierClassName = readOutDefaultSupplier();
                String supplierClassName = ((ProviderEntry) spinner.getSelectedItem()).getSupplierClassName();
                if (cb.isChecked()) {
                    saveDefaultProvider(((ProviderEntry) spinner.getSelectedItem()).getSupplierClassName());
                } else if (defaultSupplierClassName != null && defaultSupplierClassName.equals(supplierClassName)) {
                    saveDefaultProvider("");
                }
                Intent intent = new Intent(MainActivity.this, SendActivity.class);
                intent.putExtra(SendActivity.SUPPLIER_CLASS_NAME, supplierClassName);
                intent.putExtra(SendActivity.GIVEN_NUMBER, givenNumber);
                intent.putExtra(SendActivity.GIVEN_NAME, name);
                intent.putExtra(SendActivity.GIVEN_ID, id);
                SendActivity.infoMsg = null;
                startActivity(intent);
            }
        });
    }

    private void saveDefaultProvider(String defaultSupplierClass) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(GlobalPreferences.GLOBAL_DEFAULT_PROVIDER, defaultSupplierClass);
        editor.commit();
    }


    private String readOutDefaultSupplier() {
        return settings.getString(GlobalPreferences.GLOBAL_DEFAULT_PROVIDER, null);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem providerOption = menu.add(0, PROVIDER_OPTION, 0, getString(R.string.text_provider_settings_short));
        providerOption.setIcon(R.drawable.settingsbutton);
        MenuItem globalOption = menu.add(0, GLOBAL_OPTION, 0, getString(R.string.text_program_settings_short));
        globalOption.setIcon(R.drawable.settingsbutton);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PROVIDER_OPTION:
                Intent intent = new Intent(this, ProviderPreferences.class);
                intent.putExtra(ProviderPreferences.SUPPLIER_CLASS_NAME, ((ProviderEntry) spinner.getSelectedItem()).getSupplierClassName());
                startActivity(intent);
                return true;
            case GLOBAL_OPTION:
                Intent pref = new Intent(this, GlobalPreferences.class);
                startActivity(pref);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
