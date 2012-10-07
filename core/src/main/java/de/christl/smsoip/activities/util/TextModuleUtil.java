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

package de.christl.smsoip.activities.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper for saving and getting strings from SharedPreferences
 */
public abstract class TextModuleUtil {


    private static SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp().getApplicationContext());

    public static Map<String, String> getTextModules() {
        Map<String, String> out = new HashMap<String, String>();
        Map<String, ?> allPreferences = sharedPreferences.getAll();
        for (Map.Entry<String, ?> stringEntry : allPreferences.entrySet()) {
            String key = stringEntry.getKey();
            if (key.startsWith(SettingsConst.TEXT_MODULES_PREFIX)) {
                out.put(key.replaceAll(SettingsConst.TEXT_MODULES_PREFIX, ""), String.valueOf(stringEntry.getValue()));
            }
        }
        return out;
    }

    public static void updateValue(String oldKey, String newKey, String value) {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        if (oldKey != null && !oldKey.equals(newKey)) {
            edit.remove(SettingsConst.TEXT_MODULES_PREFIX + oldKey);
        }
        edit.putString(SettingsConst.TEXT_MODULES_PREFIX + newKey, value);
        edit.commit();
    }

    public static void removeKey(String key) {
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.remove(SettingsConst.TEXT_MODULES_PREFIX + key);
        edit.commit();
    }
}
