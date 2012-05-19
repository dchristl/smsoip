package de.christl.smsoip.supplier.smsde;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

public class SMSDeOptionProvider extends OptionProvider {

    private static final String providerName = "SMS.de";
    private int messageLength = 142;

    public SMSDeOptionProvider() {
        super(providerName);
    }

    @Override
    public int getMaxReceiverCount() {
        return 1;
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
                    case 0:
                        messageLength = 142;
                        break;
                    case 1:
                    case 2:
                        messageLength = 151;
                        break;
                    default:
                        messageLength = 291;
                        break;
                }
                sendActivity.updateSMScounter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
//        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, Constants.FREE));
//        spinner.setSelection(defaultPosition);
    }


}
