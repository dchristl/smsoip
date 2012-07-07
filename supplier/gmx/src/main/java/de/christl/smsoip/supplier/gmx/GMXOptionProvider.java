package de.christl.smsoip.supplier.gmx;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "GMX";
    public static final String PROVIDER_CHECKNOFREESMSAVAILABLE = "provider.checknofreesmsavailable";

    public GMXOptionProvider() {
        super(PROVIDER_NAME);
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

    @Override
    public int getMaxMessageCount() {
        return 5;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }
}
