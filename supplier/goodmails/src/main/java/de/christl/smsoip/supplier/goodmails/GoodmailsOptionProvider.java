package de.christl.smsoip.supplier.goodmails;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.supplier.goodmails.constant.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GoodmailsOptionProvider extends OptionProvider {
    private static final String providerName = "Goodmails";
    private int messageLength = 153;

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private int maxReceiverCount = 1;

    public GoodmailsOptionProvider() {
        super(providerName);
    }

    @Override
    public int getTextMessageLength() {
        return messageLength;
    }


    @Override
    public void createSpinner(final SendActivity sendActivity, Spinner spinner) {
        final String[] arraySpinner = new String[]{Constants.FREE, Constants.STANDARD, Constants.FAKE};
        spinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(sendActivity, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String currentSelection = arraySpinner[position];
                if (currentSelection.equals(Constants.FREE)) {
                    messageLength = 126;
                    maxReceiverCount = 1;
                } else {
                    messageLength = 153;
                    maxReceiverCount = Integer.MAX_VALUE;
                }
                sendActivity.updateSMScounter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, Constants.FREE));
        spinner.setSelection(defaultPosition);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();

        ListPreference listPref = new ListPreference(context);
        listPref.setEntries(new String[]{Constants.FREE, Constants.STANDARD, Constants.FAKE});
        listPref.setEntryValues(new String[]{Constants.FREE, Constants.STANDARD, Constants.FAKE});
        listPref.setDialogTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setSummary(getTextByResourceId(R.string.text_default_type_long));
        listPref.setDefaultValue(Constants.FREE);
        out.add(listPref);
        return out;
    }

    @Override
    public int getMaxReceiverCount() {
        return maxReceiverCount;
    }
}
