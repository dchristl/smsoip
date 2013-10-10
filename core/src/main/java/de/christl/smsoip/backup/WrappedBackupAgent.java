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

import android.app.backup.BackupManager;
import android.content.Context;

/**
 * Helper Class for checking availability of BackupManager
 */
class WrappedBackupAgent {


    private BackupManager wrappedInstance;

    static {
        try {
            Class.forName("android.app.backup.BackupManager");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkAvailable() {
    }

    public WrappedBackupAgent(Context context) {
        wrappedInstance = new BackupManager(context);
    }

    public void dataChanged() {
        wrappedInstance.dataChanged();
    }

//    public void restore() {
//        wrappedInstance.requestRestore(new RestoreObserver() {
//            @Override
//            public void restoreStarting(int numPackages) {
//                super.restoreStarting(numPackages);
//            }
//
//            @Override
//            public void onUpdate(int nowBeingRestored, String currentPackage) {
//                super.onUpdate(nowBeingRestored, currentPackage);
//            }
//
//            @Override
//            public void restoreFinished(int error) {
//                super.restoreFinished(error);
//            }
//        });
//    }
}
