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
import de.christl.smsoip.util.BitmapProcessor;

/**
 * calculate the new size of the image and set it as background
 */
public class ProcessImageAndSetBackgroundTask extends AsyncTask<String, Void, Boolean> {
    private Activity activity;

    public ProcessImageAndSetBackgroundTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return BitmapProcessor.decodeAndSaveImages(params[0], Integer.valueOf(params[1]));
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if (aBoolean && activity != null && !activity.isFinishing() && !isCancelled()) {
            activity.getWindow().setBackgroundDrawable(BitmapProcessor.getBackgroundImage(activity.getResources().getConfiguration().orientation));
        }
    }
}
