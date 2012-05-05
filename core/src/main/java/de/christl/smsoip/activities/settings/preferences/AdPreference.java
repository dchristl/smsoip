package de.christl.smsoip.activities.settings.preferences;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.AllActivity;

public class AdPreference extends Preference {


    public AdPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.adpreference);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        // Initiate a generic request to load it with an ad
        // the context is a PreferenceActivity
        Activity activity = (Activity) getContext();

        // Create the adView
        AdView adView = new AdView(activity, AdSize.BANNER, AllActivity.PUBLISHER_ID);

        ((LinearLayout) view).addView(adView);

        // Initiate a generic request to load it with an ad
        AdRequest request = new AdRequest();
        adView.loadAd(request);
        return view;
    }
}