package de.christl.smsoip.activities.settings.preferences.model;

import java.util.*;

/**
 * more convenient List for managing the accounts
 */
public class AccountModelsList extends LinkedList<AccountModel> {


    private AccountModel fake;

    public void put(String userName, String passWord) {
        AccountModel accountModel = new AccountModel(userName, passWord);
        add(accountModel);
    }


    /**
     * add a fake account (will not added to original values)
     *
     * @param description - description used
     */
    public void addFakeAsLast(String description) {
        fake = new AccountModel(description, "fake");
        addLast(fake);
    }

    public void removeFake() {
        remove(fake);
    }
}
