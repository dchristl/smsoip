package de.christl.smsoip.supplier.gmx;

import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String providerName = "GMX";

    public GMXOptionProvider() {
        super(providerName);
    }

    @Override
    public Class<? extends SMSSupplier> getSupplier() {
        return GMXSupplier.class;
    }
}
