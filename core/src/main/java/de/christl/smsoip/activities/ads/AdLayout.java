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

package de.christl.smsoip.activities.ads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 *
 */
public class AdLayout extends LinearLayout implements AdListener, View.OnClickListener {
    private boolean noSmartBanner;
    private ImageView imageView;
    private AdView adView;

    private boolean firstAdReceived = false;
    private final Handler refreshHandler = new Handler();

    private final Runnable refreshRunnable = new RefreshRunnable();


    public AdLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        noSmartBanner = attrs.getAttributeBooleanValue("http://schemas.android.com/apk/res/de.christl.smsoip", "noSmartBanner", false);
    }

    public AdLayout(Context context) {
        super(context);
    }

    private void init() {

        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            imageView = new ImageView(getContext());
            imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ad_layer));
            addView(imageView);
            imageView.setOnClickListener(this);
            adView = new AdView((Activity) getContext(), noSmartBanner ? AdSize.BANNER : AdSize.SMART_BANNER, "ca-app-pub-6074434370638620/1583934333");
            addView(adView);
            adView.setAdListener(this);
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
        if (!firstAdReceived) {
            // Request a new ad immediately.
            refreshHandler.post(refreshRunnable);
        }

    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Remove any pending ad refreshes.
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    public void onReceiveAd(Ad ad) {
        firstAdReceived = true;
        // Hide the custom image and show the AdView.
        imageView.setVisibility(View.GONE);
        adView.setVisibility(View.VISIBLE);

    }

    @Override
    public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode code) {
        if (!firstAdReceived) {
            // Hide the AdView and show the custom image.
            adView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);

            refreshHandler.removeCallbacks(refreshRunnable);
            refreshHandler.postDelayed(refreshRunnable, 10 * 1000);
        }
    }

    @Override
    public void onPresentScreen(Ad ad) {

    }

    @Override
    public void onDismissScreen(Ad ad) {

    }

    @Override
    public void onLeaveApplication(Ad ad) {

    }

    @Override
    public void onClick(View v) {
        try {
            String marketUrl = getContext().getString(R.string.adfree_market_url);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            //Market not available on device
            String alternativeMarketUrl = getContext().getString(R.string.adfree_alternative);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(alternativeMarketUrl));
            getContext().startActivity(intent);
        }
    }


    private class RefreshRunnable implements Runnable {
        @Override
        public void run() {
            // Load an ad with an ad request.
            if (adView != null) {
                AdRequest adRequest = new AdRequest();
                adRequest.addTestDevice("E3234EBC64876258C233EAA63EE49966");
                adView.loadAd(adRequest);
            }
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (adView != null && imageView != null) {
            removeAllViews();
            init();
            refreshHandler.post(refreshRunnable);
        }
    }

    public void destroy() {
        if (adView != null) {
            adView.destroy();
        }
    }

}
