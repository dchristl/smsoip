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
        ((LinearLayout) view).addView(adView);
        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            adView.setVisibility(View.VISIBLE);
        } else {
            adView.setVisibility(View.GONE);
        }
        return view;
    }
}