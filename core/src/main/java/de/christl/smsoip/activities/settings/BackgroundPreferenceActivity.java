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

package de.christl.smsoip.activities.settings;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.google.analytics.tracking.android.EasyTracker;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.util.BitmapProcessor;


public abstract class BackgroundPreferenceActivity extends PreferenceActivity {

    private Drawable backgroundImage;


    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SMSoIPApplication.setCurrentActivity(this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        backgroundImage = BitmapProcessor.getBackgroundImage(getResources().getConfiguration().orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        backgroundImage = BitmapProcessor.getBackgroundImage(newConfig.orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundImage != null) {
            backgroundImage.setCallback(null);
        }
    }
}
