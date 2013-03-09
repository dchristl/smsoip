/*
 * Copyright (c) Danny Christl 2013.
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

package de.christl.smsoip.activities.threading;

import android.app.Dialog;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.models.ErrorReporterStack;
import org.acra.ACRA;

/**
 * Helper for threading stuff
 */
public abstract class ThreadingUtil {

    private ThreadingUtil() {
    }

    /**
     * kill a dialog after a while
     *
     * @param dialog
     * @param timeinMillis
     */
    public static void killDialogAfterAWhile(final Dialog dialog, final long timeinMillis) {
        ErrorReporterStack.put(LogConst.KILL_DIALOG_AFTER_A_WHILE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeinMillis);
                } catch (InterruptedException ignored) {
                } finally {
                    if (dialog != null && dialog.isShowing()) {
                        try {
                            dialog.cancel();
                        } catch (Exception e) {
                            ACRA.getErrorReporter().handleSilentException(new IllegalArgumentException("Error dismissing dialog<" + dialog + ">", e));
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * dismiss the given dialog after 3 secondes
     *
     * @param dialog
     */
    public static void killDialogAfterAWhile(final Dialog dialog) {
        killDialogAfterAWhile(dialog, 3000);
    }
}
