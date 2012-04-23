package de.christl.smsoip.supplier.goodmails;

import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

/**
 *
 */
public class GoodmailsOptionProvider extends OptionProvider {
    private static final String providerName = "Goodmails";

    public GoodmailsOptionProvider() {
        super(providerName);
    }

    @Override
    public Class<? extends SMSSupplier> getSupplier() {
        return GoodmailsSupplier.class;
    }
}
