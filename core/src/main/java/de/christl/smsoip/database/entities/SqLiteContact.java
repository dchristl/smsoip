/*
 * Copyright (c) Danny Christl 2013.
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

package de.christl.smsoip.database.entities;

import java.util.ArrayList;
import java.util.List;

/**
 * Class handling one contact from custom database
 */
public class SqLiteContact {

    private Long id;
    private String firstName;
    private String lastName;
    private String plugin;
    private String user;

    private List<SqLiteNumber> numbers = new ArrayList<SqLiteNumber>();

    public SqLiteContact(Long id, String firstName, String lastName, String plugin, String user, List<SqLiteNumber> numbers) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.plugin = plugin;
        this.user = user;
        this.numbers = numbers;
    }

    public SqLiteContact(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPlugin() {
        return plugin;
    }

    public String getUser() {
        return user;
    }

    public List<SqLiteNumber> getNumbers() {
        return numbers;
    }
}
