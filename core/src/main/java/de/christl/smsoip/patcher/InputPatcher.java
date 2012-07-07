package de.christl.smsoip.patcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.option.OptionProvider;

/**
 * Patcher for changing values suring runtime
 */
public abstract class InputPatcher {


    private static final String ADD_SUPPLIER_PREFERENCE = "asp";
    private static final String ADD_GLOBAL_PREFERENCE = "agp";
    private static final String ENABLE_AUTO_UPDATE = "autoupdateenable";
    public static final String SHOW_RETURN_FROM_SERVER = "iamebenezerscrooge";

    public static boolean patchProgram(String input, OptionProvider provider) {
        if (input.startsWith(ADD_SUPPLIER_PREFERENCE)) {
            return addSupplierPreference(input, provider);
        } else if (input.startsWith(ADD_GLOBAL_PREFERENCE)) {
            return addGlobalPreference(input);
        } else if (input.equals(SHOW_RETURN_FROM_SERVER)) {
            return addSupplierPreference(ADD_SUPPLIER_PREFERENCE + " " + SHOW_RETURN_FROM_SERVER + " " + "b" + " " + "true", provider);
        } else if (input.equals(ENABLE_AUTO_UPDATE)) {
            return addGlobalPreference(ADD_GLOBAL_PREFERENCE + " " + GlobalPreferences.GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP + " " + "b" + " " + "true");
        }
        return false;
    }

    private static boolean addGlobalPreference(String input) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp().getApplicationContext());
        SharedPreferences.Editor edit = defaultSharedPreferences.edit();
        return addPreference(input, edit);
    }

    private static boolean addSupplierPreference(String input, OptionProvider provider) {
        SharedPreferences sharedPreferences = provider.getSettings();
        SharedPreferences.Editor edit = sharedPreferences.edit();
        return addPreference(input, edit);
    }

    private static boolean addPreference(String input, SharedPreferences.Editor edit) {
        String[] split = input.split(" ");
        if (split.length != 4) {
            return false;
        }
        String name = split[1];
        String type = split[2];
        String value = split[3];
        if (type.equals("b")) {
            edit.putBoolean(name, Boolean.valueOf(value));
        } else if (type.equals("s")) {
            edit.putString(name, value);
        } else if (type.equals("i")) {
            try {
                edit.putInt(name, Integer.valueOf(value));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return edit.commit();
    }
}
