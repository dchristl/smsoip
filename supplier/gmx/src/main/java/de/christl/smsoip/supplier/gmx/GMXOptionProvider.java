package de.christl.smsoip.supplier.gmx;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String providerName = "GMX";
    public static final String PROVIDER_CHECKNOFREESMSAVAILABLE = "provider.checknofreesmsavailable";

    public GMXOptionProvider() {
        super(providerName);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
        CheckBoxPreference checkNoFreeSMSAvailable = new CheckBoxPreference(context);
        checkNoFreeSMSAvailable.setKey(PROVIDER_CHECKNOFREESMSAVAILABLE);
        checkNoFreeSMSAvailable.setTitle(getTextByResourceId(R.string.text_check_no_free_available));
        checkNoFreeSMSAvailable.setSummary(getTextByResourceId(R.string.text_check_no_free_available_long));
        out.add(checkNoFreeSMSAvailable);
        return out;
    }


}
