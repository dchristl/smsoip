/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

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
