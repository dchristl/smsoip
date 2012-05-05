package de.christl.smsoip.supplier.goodmails;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.supplier.goodmails.constant.Constants;

/**
 *
 */
public class GoodmailsOptionProvider extends OptionProvider {
    private static final String providerName = "Goodmails";
    private int messageLength = 153;

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
                } else {
                    messageLength = 153;
                }
                sendActivity.updateSMScounter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
    }
}
