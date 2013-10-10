/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.util.Map;

import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.util.BitmapProcessor;

/**
 *
 */
public class BackupAgent extends BackupAgentHelper {


    private static final String PREFS_BACKUP_KEY = "preferences";
    private static final String FILES_BACKUP_KEY = "background_images";
    private static final String GLOBAL_PREFERENCES = "de.christl.smsoip_preferences";
    private static final String RATING_PREFERENCES = "de.christl.smsoip.rating";
    private static final String PORTRAIT = "background_portrait";
    private static final String LANDSCAPE = "background_landscape";

    @Override
    public void onCreate() {
        Map<String, SMSoIPPlugin> providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        String[] optionEntries = new String[providerEntries.size() + 2];
        int i = 0;
        for (SMSoIPPlugin smSoIPPlugin : providerEntries.values()) {
            String preferenceName = smSoIPPlugin.getProvider().getClass().getCanonicalName() + "_preferences";
            optionEntries[i] = preferenceName;
            i++;
        }
        optionEntries[i] = GLOBAL_PREFERENCES;
        optionEntries[++i] = RATING_PREFERENCES;

        SharedPreferencesBackupHelper preferencesBackupHelper = new SharedPreferencesBackupHelper(this, optionEntries);
        addHelper(PREFS_BACKUP_KEY, preferencesBackupHelper);

        FileBackupHelper fileBackupHelper = new FileBackupHelper(this, PORTRAIT, LANDSCAPE);
        addHelper(FILES_BACKUP_KEY, fileBackupHelper);
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        synchronized (BitmapProcessor.LOCK) {
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        synchronized (BitmapProcessor.LOCK) {
            super.onRestore(data, appVersionCode, newState);
        }
    }
}
