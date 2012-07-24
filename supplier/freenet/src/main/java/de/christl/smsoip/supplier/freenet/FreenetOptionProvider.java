package de.christl.smsoip.supplier.freenet;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for Freenet options
 */
public class FreenetOptionProvider extends OptionProvider {

    private static String providerName = "Freenet";

    public static final String PROVIDER_SAVE_IN_SENT = "provider.saveInSent";

    public FreenetOptionProvider() {
        super(providerName);
    }


    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
        CheckBoxPreference checkNoFreeSMSAvailable = new CheckBoxPreference(context);
        checkNoFreeSMSAvailable.setKey(PROVIDER_SAVE_IN_SENT);
        checkNoFreeSMSAvailable.setTitle(getTextByResourceId(R.string.text_save_in_sent));
        checkNoFreeSMSAvailable.setSummary(getTextByResourceId(R.string.text_save_in_sent_long));
        out.add(checkNoFreeSMSAvailable);
        return out;
    }
}
