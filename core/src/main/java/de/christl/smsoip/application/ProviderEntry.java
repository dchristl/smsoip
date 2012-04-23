package de.christl.smsoip.application;

import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

/**
 *
 */
public class ProviderEntry {

    private final String providerName;
    private final Class<? extends SMSSupplier> supplier;
    private String optionProviderClassName;

    public ProviderEntry(OptionProvider optionProviderClassName) {
        this.optionProviderClassName = optionProviderClassName.getClass().getCanonicalName();
        providerName = optionProviderClassName.getProviderName();
        supplier = optionProviderClassName.getSupplier();
    }

    public String getProviderName() {
        return providerName;
    }

    public String getSupplierClassName() {
        return supplier.getCanonicalName();
    }

    public String getOptionProviderClassName() {
        return optionProviderClassName;
    }
}
