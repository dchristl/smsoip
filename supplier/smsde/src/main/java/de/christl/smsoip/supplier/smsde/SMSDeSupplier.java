package de.christl.smsoip.supplier.smsde;


import android.text.Editable;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.List;

public class SMSDeSupplier implements SMSSupplier {


    private SMSDeOptionProvider provider;

    public SMSDeSupplier() {
        provider = new SMSDeOptionProvider();
    }

    @Override
    public Result refreshInformationOnRefreshButtonPressed() {
        return Result.NO_ERROR;
    }

    @Override
    public Result refreshInformationAfterMessageSuccessfulSent() {
        return Result.NO_ERROR;
    }

    @Override
    public String getProviderInfo() {
        return provider.getProviderName();
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
    public Result login(String userName, String password) {
        return Result.NO_ERROR;
    }

    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        return Result.NO_ERROR;
    }
}
