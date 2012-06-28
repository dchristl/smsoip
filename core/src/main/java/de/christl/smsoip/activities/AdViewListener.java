package de.christl.smsoip.activities;

import android.view.View;
import com.mobclix.android.sdk.MobclixAdView;
import com.mobclix.android.sdk.MobclixAdViewListener;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 * Created with IntelliJ IDEA.
 * User: Danny
 * Date: 28.06.12
 * Time: 20:10
 * To change this template use File | Settings | File Templates.
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
