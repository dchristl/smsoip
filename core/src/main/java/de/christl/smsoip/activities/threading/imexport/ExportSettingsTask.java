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

import java.io.File;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.threading.ThreadingUtil;

/**
 *
 */
public class ExportSettingsTask extends AsyncTask<Void, Void, Boolean> {


    private final ProgressDialog progressDialog;
    private Context context;

    public ExportSettingsTask(Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        String message = context.getString(R.string.settings_will_be_exported);
        progressDialog.setMessage(String.format(message, ImExportHelper.getDataDir(context).getAbsolutePath()));
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
        if (!ImExportHelper.cleanupAndCreate(exportDir)) {
            return false;
        }


        boolean success = true;
        File[] files = dataDir.listFiles();
        if (files == null) {
            return false;
        }
        for (File file : files) {
            String fileName = file.getName();
            if (fileName.endsWith(".xml") && !fileName.contains("MCConfig")) {
                success &= ImExportHelper.copyFileToDir(exportDir, file);
            }
        }


        return success;
    }


    @Override
    protected void onPostExecute(Boolean success) {
        String message;
        if (success) {
            message = context.getString(R.string.export_success);
        } else {
            message = context.getString(R.string.export_failure);
        }
        progressDialog.setMessage(message);
        ThreadingUtil.killDialogAfterAWhile(progressDialog, 2000);
    }


}
