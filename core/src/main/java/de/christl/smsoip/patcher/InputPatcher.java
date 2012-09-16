/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.patcher;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.activities.threading.UpdateDeveloperInfoTask;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.util.BitmapProcessor;

/**
 * Patcher for changing values suring runtime
 */
public abstract class InputPatcher {


    private static final String ADD_SUPPLIER_PREFERENCE = "asp";
    private static final String ADD_GLOBAL_PREFERENCE = "agp";
    private static final String ENABLE_AUTO_UPDATE = "autoupdateenable";
    public static final String SHOW_RETURN_FROM_SERVER = "iamebenezerscrooge";
    public static final String REMOVE_BACKGROUND_IMAGES = "rm bg";
    public static final String SET_DEV_FLAG = "devenable";

    public static boolean patchProgram(String input, OptionProvider provider) {
        if (input.startsWith(ADD_SUPPLIER_PREFERENCE)) {
            return addSupplierPreference(input, provider);
        } else if (input.startsWith(ADD_GLOBAL_PREFERENCE)) {
            return addGlobalPreference(input);
        } else if (input.equals(SHOW_RETURN_FROM_SERVER)) {
            return addSupplierPreference(ADD_SUPPLIER_PREFERENCE + " " + SHOW_RETURN_FROM_SERVER + " " + "b" + " " + "true", provider);
        } else if (input.equals(ENABLE_AUTO_UPDATE)) {
            return addGlobalPreference(ADD_GLOBAL_PREFERENCE + " " + SettingsConst.GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP + " " + "b" + " " + "true");
        } else if (input.equals(REMOVE_BACKGROUND_IMAGES)) {
            BitmapProcessor.removeBackgroundImages();
            return true;
        } else if (input.equals(SET_DEV_FLAG)) {
            return addGlobalPreference(ADD_GLOBAL_PREFERENCE + " " + UpdateDeveloperInfoTask.NOTIFICATION_IS_DEV + " " + "b" + " " + "true");
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
