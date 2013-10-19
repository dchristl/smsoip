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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.AdView;

import de.christl.smsoip.R;

/**
 * Simple listener handling visibility of ads by success/error
 */
public class AdViewListener implements AdListener {

    private Context context;
    private ImageView imageView;

    public AdViewListener(Context context) {
        this.context = context;
    }

    @Override
    public void onReceiveAd(Ad ad) {
        ((AdView) ad).setVisibility(View.VISIBLE);
        if (imageView != null) {
            ViewGroup parent = (ViewGroup) imageView.getParent();
            if (parent != null) {
                parent.removeView(imageView);
            }
            imageView.invalidate();
        }
    }

    @Override
    public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {
        ((AdView) ad).setVisibility(View.VISIBLE);
        if (imageView == null) {
            imageView = new ImageView(context);
            imageView.setBackgroundResource(R.drawable.ad_layer);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        String marketUrl = context.getString(R.string.adfree_market_url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
                        context.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        //Market not available on device
                        String alternativeMarketUrl = context.getString(R.string.adfree_alternative);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(alternativeMarketUrl));
                        context.startActivity(intent);
                    }
                }
            });
            ((AdView) ad).addView(imageView);
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

//    @Override
//    public void onSuccessfulLoad(MobclixAdView mobclixAdView) {
//        mobclixAdView.setVisibility(View.VISIBLE);
//        if (imageView != null) {
//            ((ViewGroup) imageView.getParent()).removeView(imageView);
//            imageView.invalidate();
//        }
//        mobclixAdView.setRefreshTime(10000);
//    }
//
//    @Override
//    public void onFailedLoad(MobclixAdView mobclixAdView, int i) {
//        mobclixAdView.setVisibility(View.VISIBLE);
//        if (imageView == null) {
//            imageView = new ImageView(context);
//            imageView.setBackgroundResource(R.drawable.ad_layer);
//            imageView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    try {
//                        String marketUrl = context.getString(R.string.adfree_market_url);
//                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
//                        context.startActivity(intent);
//                    } catch (ActivityNotFoundException e) {
//                        //Market not available on device
//                        String alternativeMarketUrl = context.getString(R.string.adfree_alternative);
//                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(alternativeMarketUrl));
//                        context.startActivity(intent);
//                    }
//                }
//            });
//            mobclixAdView.addView(imageView);
//        }
//        mobclixAdView.setRefreshTime(10000);
//
//    }
//
//    @Override
//    public void onAdClick(MobclixAdView mobclixAdView) {
//
//    }
//
//    @Override
//    public boolean onOpenAllocationLoad(MobclixAdView mobclixAdView, int i) {
//        return false;
//    }
//
//    @Override
//    public void onCustomAdTouchThrough(MobclixAdView mobclixAdView, String s) {
//
//    }
//
//    @Override
//    public String keywords() {
//        return null;
//    }
//
//    @Override
//    public String query() {
//        return null;
//    }
}
