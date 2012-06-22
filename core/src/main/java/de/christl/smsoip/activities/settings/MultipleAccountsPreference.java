package de.christl.smsoip.activities.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

/**
 *
 */
public class MultipleAccountsPreference extends DialogPreference {
    public MultipleAccountsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setPersistent(false);
        SharedPreferences sharedPreferences = getSharedPreferences();
//        myView.setValue1(sharedPreferences.getString(myKey1, myDefaultValue1));
//        myView.setValue2(sharedPreferences.getString(myKey2, myDefaultValue2));
    }

    public MultipleAccountsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);//update values
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
//            persistBoolean() valuse
        }
    }
}
