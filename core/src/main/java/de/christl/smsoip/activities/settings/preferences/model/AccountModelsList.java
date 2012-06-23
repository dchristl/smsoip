package de.christl.smsoip.activities.settings.preferences.model;

import java.util.*;

/**
 * more convenient List for managing the accounts
 */
public class AccountModelsList extends LinkedList<AccountModel> {


    public void put(String userName, String passWord) {
        AccountModel accountModel = new AccountModel(userName, passWord);
        add(accountModel);
    }


    public CharSequence[] getKeys() {
        CharSequence[] out = new CharSequence[size()];
        for (int i = 0, nullSize = size(); i < nullSize; i++) {
            out[i++] = String.valueOf(i);
        }
        return out;
    }

    public CharSequence[] getValues() {
        CharSequence[] out = new CharSequence[size()];
        int i = 0;
        for (AccountModel model : this) {
            out[i++] = model.getUserName();
        }
        return out;
    }


//    public void cloneValues() {
////        temporaryValues.clear();
////        for (Map.Entry<Integer, AccountModel> integerAccountModelsListEntry : originalValues.entrySet()) {
////            temporaryValues.put(integerAccountModelsListEntry.getKey(), integerAccountModelsListEntry.getValue().cloneSilent());
////        }
//    }

    /**
     * add a fake account (will not added to original values)
     *
     * @param description
     */
    public void addFakeAsLast(String description) {
        addLast(new AccountModel(description, "fake"));
    }
}
