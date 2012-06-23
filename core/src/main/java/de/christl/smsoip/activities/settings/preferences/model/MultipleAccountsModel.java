package de.christl.smsoip.activities.settings.preferences.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @deprecated check if can be reomoved
 */
public class MultipleAccountsModel extends LinkedHashMap<Integer, AccountModel> {

//    private MultipleAccountsModel originalValues = new MultipleAccountsModel();
//    private MultipleAccountsModel temporaryValues = new MultipleAccountsModel();

    public void put(int i, String userName, String passWord) {
        put(i, new AccountModel(0, userName, passWord));
    }

    public CharSequence[] getKeys() {
        CharSequence[] out = new CharSequence[size()];
        int i = 0;
        for (Integer integer : keySet()) {
            out[i++] = String.valueOf(integer);
        }
        return out;
    }

    public CharSequence[] getValues() {
        CharSequence[] out = new CharSequence[size()];
        int i = 0;
        for (AccountModel model : values()) {
            out[i++] = model.getUserName();
        }
        return out;
    }

    public List<AccountModel> getOriginalValues() {
        return new ArrayList<AccountModel>(values());
    }


}
