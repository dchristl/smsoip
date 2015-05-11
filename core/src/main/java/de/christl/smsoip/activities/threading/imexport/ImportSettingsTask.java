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

package de.christl.smsoip.activities.threading.imexport;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.AllActivity;
import de.christl.smsoip.activities.threading.ThreadingUtil;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.backup.BackupHelper;
import de.christl.smsoip.constant.TrackerConstants;

/**
 *
 */
public class ImportSettingsTask extends AsyncTask<Void, Void, Boolean> {

    private final ProgressDialog progressDialog;
    private final Context context;

    public ImportSettingsTask(Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        String message = context.getString(R.string.settings_will_be_imported);
        File file = new File(ImExportHelper.getExportDir(), ImExportHelper.ZIP_FILE_NAME);
        progressDialog.setMessage(String.format(message, file.getAbsolutePath()));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        File dataDir = ImExportHelper.getDataDir(context);
        if (!dataDir.exists()) {
            return false;
        }

        File exportDir = ImExportHelper.getExportDir();
        if (exportDir == null) {
            return false;
        }

        boolean success = extractZipFile(dataDir, exportDir);
        Tracker tracker = SMSoIPApplication.getApp().getTracker();
        HitBuilders.EventBuilder builder = new HitBuilders.EventBuilder().setCategory(TrackerConstants.CAT_BUTTONS).setAction(TrackerConstants.EVENT_IMPORT).setLabel(String.valueOf(success));
        tracker.send(builder.build());
        return success;
    }

    private boolean extractZipFile(File dataDir, File exportDir) {
        byte[] buffer = new byte[1024];
        boolean success = true;
        ZipInputStream zis = null;
        try {
            File file = new File(exportDir, ImExportHelper.ZIP_FILE_NAME);
            if (!file.exists()) {
                return false;
            }
            zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile;
                if (fileName.endsWith(".xml")) {
                    newFile = new File(dataDir, fileName);
                } else {
                    newFile = new File(context.getFilesDir(), fileName);
                }
                FileOutputStream fos = new FileOutputStream(newFile);

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();

        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
            success = false;
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException ignored) {
                }
            }
        }
        return success;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        String message;
        if (success) {
            message = context.getString(R.string.import_success);
            BackupHelper.dataChanged();
        } else {
            message = context.getString(R.string.import_failure);
        }
        progressDialog.setMessage(message);
        ThreadingUtil.killDialogAfterAWhile(progressDialog, 2000);
        if (success) {
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    } finally {
                        AllActivity.killHard();
                    }
                }
            };
            new Thread(runnable).start();
        }
    }
}
