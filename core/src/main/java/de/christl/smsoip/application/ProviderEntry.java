package de.christl.smsoip.application;

import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

/**
 *
 */
public class ProviderEntry {

    private final ExtendedSMSSupplier supplier;
    private int minAPIVersion;
    private OptionProvider provider;

    public ProviderEntry(ExtendedSMSSupplier smsSupplier, int minAPIVersion) {
        supplier = smsSupplier;
        this.minAPIVersion = minAPIVersion;
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

    public int getMinAPIVersion() {
        return minAPIVersion;
    }
}
