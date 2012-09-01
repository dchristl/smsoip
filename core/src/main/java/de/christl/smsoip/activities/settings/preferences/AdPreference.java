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

package de.christl.smsoip.activities.settings.preferences;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.mobclix.android.sdk.MobclixAdView;
import com.mobclix.android.sdk.MobclixMMABannerXLAdView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.AdViewListener;
import de.christl.smsoip.application.SMSoIPApplication;

public class AdPreference extends Preference {


    public AdPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.adpreference);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        Activity activity = (Activity) getContext();

        MobclixAdView adView = new MobclixMMABannerXLAdView(activity);
        adView.setRefreshTime(10000);
        adView.addMobclixAdViewListener(new AdViewListener());
        ((LinearLayout) view).addView(adView);
        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            adView.setVisibility(View.VISIBLE);
        } else {
            adView.setVisibility(View.GONE);
            adView.cancelAd();
        }
        return view;
    }
}