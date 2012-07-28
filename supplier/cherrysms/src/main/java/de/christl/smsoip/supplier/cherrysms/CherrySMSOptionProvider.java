package de.christl.smsoip.supplier.cherrysms;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Options for Cherry-SMS
 */
public class CherrySMSOptionProvider extends OptionProvider {


    private static final String providerName = "Cherry-SMS";

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";

    private static final String SUPPORT_URL = "http://www.cherry-sms.com/?ref=RBSYJMGF";

    public CherrySMSOptionProvider() {
        super(providerName);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public void createSpinner(SendActivity sendActivity, Spinner spinner) {
        final String[] arraySpinner = getArrayByResourceId(R.array.array_spinner);
        spinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(sendActivity, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, arraySpinner[0]));
        defaultPosition = (defaultPosition == -1) ? 0 : defaultPosition;
        spinner.setSelection(defaultPosition);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();

        ListPreference listPref = new ListPreference(context);
        String[] typeArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(typeArray);
        listPref.setEntryValues(typeArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setSummary(getTextByResourceId(R.string.text_default_type_long));
        listPref.setDefaultValue(typeArray[0]);
        out.add(listPref);
        PreferenceScreen intentPref = ((PreferenceActivity) context).getPreferenceManager().createPreferenceScreen(context);
        intentPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(SUPPORT_URL)));
        intentPref.setTitle(getTextByResourceId(R.string.text_need_account));
        intentPref.setSummary(getTextByResourceId(R.string.text_need_account_long));
        out.add(intentPref);
        return out;
    }

    @Override
    public int getMaxMessageCount() {
        return 1;
    }

    static String getMD5String(String utf8String) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] bytesOfMessage = utf8String.getBytes("ISO-8859-1");
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] thedigest = md.digest(bytesOfMessage);
        StringBuilder hexString = new StringBuilder();
        for (byte aThedigest : thedigest) {
            String hexStringRaw = Integer.toHexString(0xFF & aThedigest);
            hexString.append(("00" + hexStringRaw).substring(hexStringRaw.length()));   //add leading zero to String
        }

        return hexString.toString();
    }
}
