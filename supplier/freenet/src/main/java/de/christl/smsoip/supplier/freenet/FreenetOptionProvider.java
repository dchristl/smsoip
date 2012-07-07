package de.christl.smsoip.supplier.freenet;

import android.graphics.drawable.Drawable;
import de.christl.smsoip.option.OptionProvider;

/**
 * Class for Freenet options
 */
public class FreenetOptionProvider extends OptionProvider {

    private static String providerName = "Freenet";

    public FreenetOptionProvider() {
        super(providerName);
    }


    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }
}
