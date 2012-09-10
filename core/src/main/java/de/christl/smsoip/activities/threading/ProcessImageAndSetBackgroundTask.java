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

package de.christl.smsoip.activities.threading;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;
import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.util.BitmapProcessor;

/**
 * calculate the new size of the image and set it as background
 */
public class ProcessImageAndSetBackgroundTask extends AsyncTask<String, Void, Boolean> {


    @Override
    protected Boolean doInBackground(String... params) {
        return BitmapProcessor.decodeAndSaveImages(params[0], Integer.valueOf(params[1]));
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        Activity activity = SMSoIPApplication.getCurrentActivity();
        if (activity != null && !activity.isFinishing() && !isCancelled()) {
            if (aBoolean) {
                activity.getWindow().setBackgroundDrawable(BitmapProcessor.getBackgroundImage(activity.getResources().getConfiguration().orientation));
            } else {
                Toast errorToast = Toast.makeText(activity, R.string.text_background_set_error, Toast.LENGTH_LONG);
                errorToast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
                errorToast.show();
            }
        }
    }
}
