package de.christl.smsoip.supplier.smsde;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

public class SMSDeOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "SMS.de";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private int messageLength = 142;
    private int maxReceivers = 1;

    public SMSDeOptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public int getMaxReceiverCount() {
        return maxReceivers;
    }

    @Override
    public int getTextMessageLength() {
        return messageLength;
    }

    @Override
    public void createSpinner(final SendActivity sendActivity, Spinner spinner) {
        final String[] arraySpinner = getArrayByResourceId(R.array.array_spinner);
        spinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(sendActivity, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:        //free
                        messageLength = 151;
                        maxReceivers = 1;
                        break;
                    case 1:    //power sms 160
                    case 2:
                        messageLength = 160;
                        maxReceivers = Integer.MAX_VALUE;
                        break;
                    default:                  //power sms 300
                        messageLength = 300;
                        maxReceivers = Integer.MAX_VALUE;
                        break;
                }
                sendActivity.updateSMScounter();
                sendActivity.updateAfterReceiverCountChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, arraySpinner[0]));
        spinner.setSelection(defaultPosition);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();

        ListPreference listPref = new ListPreference(context);
        String[] prefArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(prefArray);
        listPref.setEntryValues(prefArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setSummary(getTextByResourceId(R.string.text_default_type_long));
        listPref.setDefaultValue(prefArray[0]);
        out.add(listPref);
        return out;
    }

    @Override
    public int getMaxMessageCount() {
        return 1;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }
}
