package de.christl.smsoip.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import de.christl.smsoip.R;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.List;

public class MainActivity extends AllActivity {

    private int OPTION_MENU = 2;
    private Spinner spinner;
    public static final String FILENAME = "option";
    public String[] array_spinner;
    public static final String PARAMETER = "nonskip";
    private String givenNumber;
    private List<ProviderEntry> providerEntries;
    private SharedPreferences settings;
    static final String DEFAULT_SUPPLIER_CLASS = "defaultProvider";


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        givenNumber = getIntent().getData() != null ? getIntent().getData().getSchemeSpecificPart() : null;
        setContentView(R.layout.main);
        Bundle extras = getIntent().getExtras();
        Boolean nonskip = extras == null ? null : (Boolean) extras.get(PARAMETER);
        String defaultSupplierClass = readOutDefaultSupplier();
        spinner = (Spinner) findViewById(R.id.providerSpinner);
        providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        int providersSize = providerEntries.size();
        if (providersSize == 0) {
            return;
        }
        if (providersSize == 1) {
            nonskip = nonskip == null ? false : nonskip;
            defaultSupplierClass = providerEntries.get(0).getSupplierClassName();
            saveSetting(defaultSupplierClass);
        }
        array_spinner = new String[providersSize];
        for (int i = 0; i < providersSize; i++) {
            ProviderEntry providerEntry = providerEntries.get(i);
            array_spinner[i] = providerEntry.getProviderName();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, array_spinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (nonskip != null && nonskip) {
            if (defaultSupplierClass != null) {
                ((CheckBox) findViewById(R.id.defaultCheckBox)).setChecked(true);
                for (int i = 0; i < array_spinner.length; i++) {
                    if (array_spinner[i].equals(defaultSupplierClass)) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        } else {

            if (defaultSupplierClass != null) {
                Intent intent = new Intent(MainActivity.this, SendActivity.class);
                intent.putExtra(SendActivity.SUPPLIER_CLASS_NAME, defaultSupplierClass);
                intent.putExtra(SendActivity.GIVEN_NUMBER, givenNumber);
                SendActivity.infoMsg = null;
                startActivity(intent);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selection = providerEntries.get(i).getSupplierClassName();
                String defaultSupplierClassName = readOutDefaultSupplier();
                boolean checked = defaultSupplierClassName != null && defaultSupplierClassName.equals(selection);
                ((CheckBox) findViewById(R.id.defaultCheckBox)).setChecked(checked);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        Button next = (Button) findViewById(R.id.nextButton);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                CheckBox cb = (CheckBox) findViewById(R.id.defaultCheckBox);
                String defaultSupplierClassName = readOutDefaultSupplier();
                String supplierClassName = providerEntries.get(spinner.getSelectedItemPosition()).getSupplierClassName();
                if (cb.isChecked()) {
                    saveSetting(MainActivity.this.providerEntries.get(MainActivity.this.spinner.getSelectedItemPosition()).getSupplierClassName());
                } else if (defaultSupplierClassName != null && defaultSupplierClassName.equals(supplierClassName)) {
                    deleteSetting();
                }
                Intent intent = new Intent(MainActivity.this, SendActivity.class);
                intent.putExtra(SendActivity.SUPPLIER_CLASS_NAME, supplierClassName);
                intent.putExtra(SendActivity.GIVEN_NUMBER, givenNumber);
                SendActivity.infoMsg = null;
                startActivity(intent);
            }
        });
        insertAds((LinearLayout) findViewById(R.id.linearLayout), this);
    }


    private String readOutDefaultSupplier() {
        return settings.getString(DEFAULT_SUPPLIER_CLASS, null);
    }


    private void saveSetting(String supplierClassName) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(DEFAULT_SUPPLIER_CLASS, supplierClassName);
        editor.commit();
    }

    private void deleteSetting() {
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(DEFAULT_SUPPLIER_CLASS);
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, OPTION_MENU, 0, getString(R.string.text_option));
        item.setIcon(R.drawable.settingsbutton);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == OPTION_MENU) {
            Intent intent = new Intent(this, OptionActivity.class);
            intent.putExtra(OptionActivity.SUPPLIER_CLASS_NAME, providerEntries.get(spinner.getSelectedItemPosition()).getSupplierClassName());
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
