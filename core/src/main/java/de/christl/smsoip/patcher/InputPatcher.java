package de.christl.smsoip.patcher;

import android.content.SharedPreferences;
import de.christl.smsoip.option.OptionProvider;

/**
 * Patcher for changing values suring runtime
 */
public abstract class InputPatcher {

    public static final String DISABLE_PAID_SMS_IN_SMS_DE = "iamebenezerscrooge";

    public static boolean patchProgram(String s, OptionProvider provider) {
        if (s.equals(DISABLE_PAID_SMS_IN_SMS_DE)) {
            if (!provider.getProviderName().equals("SMS.de")) {
                return false;
            }
            SharedPreferences sharedPreferences = provider.getSettings();
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean(DISABLE_PAID_SMS_IN_SMS_DE, true);
            edit.commit();
            return true;
        }
        return false;
    }
}
