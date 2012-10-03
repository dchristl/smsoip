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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.mobclix.android.sdk.MobclixAdView;
import com.mobclix.android.sdk.MobclixAdViewListener;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.GlobalPreferences;

/**
 * Simple listener handling visibility of ads by success/error
 */
public class AdViewListener implements MobclixAdViewListener {

    private Context context;
    private ImageView imageView;

    public AdViewListener(Context context) {
        this.context = context;
    }

    @Override
    public void onSuccessfulLoad(MobclixAdView mobclixAdView) {
        Log.e("christl", "mobclixAdView success= " + mobclixAdView);
        mobclixAdView.setVisibility(View.VISIBLE);
        mobclixAdView.removeView(imageView);
    }

    @Override
    public void onFailedLoad(MobclixAdView mobclixAdView, int i) {
        Log.e("christl", "errorcode = " + i);
        mobclixAdView.setVisibility(View.VISIBLE);
        imageView = new ImageView(context);
        imageView.setBackgroundResource(R.drawable.ad_layer);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GlobalPreferences.ADFREE_MARKET_URL));
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Market not available on device
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GlobalPreferences.WEB_ADFREE_MARKET_URL));
                    context.startActivity(intent);
                }
            }
        });
        mobclixAdView.addView(imageView);
        mobclixAdView.setRefreshTime(10000);

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
