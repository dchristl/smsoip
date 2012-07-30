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

package de.christl.smsoip.activities;

import android.view.View;
import com.mobclix.android.sdk.MobclixAdView;
import com.mobclix.android.sdk.MobclixAdViewListener;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 * Simple listener handling visibility of ads by success/error
 */
public class AdViewListener implements MobclixAdViewListener {

    public AdViewListener() {
    }

    @Override
    public void onSuccessfulLoad(MobclixAdView mobclixAdView) {
        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            mobclixAdView.setVisibility(View.VISIBLE);
        } else {
            mobclixAdView.setVisibility(View.GONE);
            mobclixAdView.cancelAd();
        }
    }

    @Override
    public void onFailedLoad(MobclixAdView mobclixAdView, int i) {
        mobclixAdView.setVisibility(View.GONE);
    }

    @Override
    public void onAdClick(MobclixAdView mobclixAdView) {

    }

    @Override
    public boolean onOpenAllocationLoad(MobclixAdView mobclixAdView, int i) {
        return false;
    }

    @Override
    public void onCustomAdTouchThrough(MobclixAdView mobclixAdView, String s) {

    }

    @Override
    public String keywords() {
        return null;
    }

    @Override
    public String query() {
        return null;
    }
}
