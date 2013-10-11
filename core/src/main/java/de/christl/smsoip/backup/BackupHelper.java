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

import de.christl.smsoip.application.SMSoIPApplication;

/**
 *
 */
public class BackupHelper {

    private static Boolean backupManagerAvailable = null;

    private static void initAvailability() {
        if (backupManagerAvailable == null) {
            try {
                WrappedBackupAgent.checkAvailable();
                backupManagerAvailable = true;
            } catch (Throwable t) {
                backupManagerAvailable = false;
            }
        }
    }

    public static void dataChanged() {

        initAvailability();

        if (backupManagerAvailable) {
            WrappedBackupAgent wrapBackupManager = new WrappedBackupAgent(SMSoIPApplication.getApp());
            wrapBackupManager.dataChanged();
        }

    }


    public static void restore() {
        initAvailability();
        if (backupManagerAvailable) {
            WrappedBackupAgent wrapBackupManager = new WrappedBackupAgent(SMSoIPApplication.getApp());
            wrapBackupManager.restore();
        }
    }

}

