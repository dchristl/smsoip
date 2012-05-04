package de.christl.smsoip.application;

import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

/**
 *
 */
public class ProviderEntry {

    private final SMSSupplier supplier;
    private OptionProvider provider;

    public ProviderEntry(SMSSupplier smsSupplier) {
        supplier = smsSupplier;
        provider = smsSupplier.getProvider();
    }

    public String getProviderName() {
        return provider.getProviderName();
    }

    public String getSupplierClassName() {
        return supplier.getClass().getCanonicalName();
    }

    @Override //this will be called by spinner for the entries
    public String toString() {
        return provider.getProviderName();
    }
}
