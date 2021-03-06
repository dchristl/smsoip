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

package de.christl.smsoip.database;

import org.apache.http.message.BasicNameValuePair;

import java.util.LinkedList;

/**
 * This is a database view of one contact with all numbers
 */
public class Contact {
    private final String name;
    private LinkedList<BasicNameValuePair> numberTypeList = new LinkedList<BasicNameValuePair>();

    public Contact(String name) {
        this.name = name;
    }

    public void addNumber(String number, String numberType) {
        numberTypeList.add(new BasicNameValuePair(number, numberType));
    }

    public LinkedList<BasicNameValuePair> getNumberTypeList() {
        return numberTypeList;
    }

    public String getName() {
        return name;
    }

}
