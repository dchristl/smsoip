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

/**
 * Class handles a number of a contac
 */
public class SqLiteNumber {
    private Long id;
    private long contactId;
    private String number;
    private Integer numberType;

    public SqLiteNumber(Long id, long contactId, String number, Integer numberType) {
        this.id = id;
        this.contactId = contactId;
        this.number = number;
        this.numberType = numberType;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setNumberType(Integer numberType) {
        this.numberType = numberType;
    }
}
