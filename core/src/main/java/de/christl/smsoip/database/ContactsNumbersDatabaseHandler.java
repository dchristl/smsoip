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

package de.christl.smsoip.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.christl.smsoip.database.entities.SqLiteContact;
import de.christl.smsoip.database.entities.SqLiteNumber;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract DAO class for managing access to contacts and numbers
 */
public abstract class ContactsNumbersDatabaseHandler {

    private ContactsNumbersDatabaseHandler() {
    }

    /**
     * fetch all contacts from own database
     *
     * @param context
     * @return
     */
    public static List<SqLiteContact> getAllContacts(Context context) {
        SQLiteDatabase contactDB = new ContactsSQLiteHelper(context).getReadableDatabase();
        List<SqLiteContact> contacts = new ArrayList<SqLiteContact>();

        Cursor contactCur = contactDB.query(ContactsSQLiteHelper.TABLE_NAME, ContactsSQLiteHelper.ALL_COLUMNS, null, null, null, null, null);

        while (contactCur != null && contactCur.moveToNext()) {
            long id = contactCur.getLong(contactCur.getColumnIndex(ContactsSQLiteHelper._ID));
            String firstName = contactCur.getString(contactCur.getColumnIndex(ContactsSQLiteHelper.FIRST_NAME));
            String lastName = contactCur.getString(contactCur.getColumnIndex(ContactsSQLiteHelper.LAST_NAME));
            String plugin = contactCur.getString(contactCur.getColumnIndex(ContactsSQLiteHelper.IMPORTED_PLUGIN));
            String user = contactCur.getString(contactCur.getColumnIndex(ContactsSQLiteHelper.IMPORTED_USER));
            List<SqLiteNumber> numbers = getAllNumbersForContactById(context, id);
            contacts.add(new SqLiteContact(id, firstName, lastName, plugin, user, numbers));

        }
        if (contactCur != null) {
            contactCur.close();
        }

        return contacts;
    }

    /**
     * get all the numbers for given contact
     *
     * @param context
     * @param id
     * @return
     */
    public static List<SqLiteNumber> getAllNumbersForContactById(Context context, long id) {
        SQLiteDatabase numberDB = new NumbersSQLiteHelper(context).getReadableDatabase();
        List<SqLiteNumber> numbers = new ArrayList<SqLiteNumber>();
        Cursor numberCur = numberDB.query(NumbersSQLiteHelper.TABLE_NAME, NumbersSQLiteHelper.ALL_COLUMNS, NumbersSQLiteHelper.CONTACT_ID + " = " + id, null, null, null, null);
        while (numberCur != null && numberCur.moveToNext()) {
            long numberId = numberCur.getLong(numberCur.getColumnIndex(NumbersSQLiteHelper._ID));
            String number = numberCur.getString(numberCur.getColumnIndex(NumbersSQLiteHelper.NUMBER));
            int numberType = numberCur.getInt(numberCur.getColumnIndex(NumbersSQLiteHelper.NUMBER_TYPE));
            numbers.add(new SqLiteNumber(numberId, id, number, numberType));
        }

        if (numberCur != null) {
            numberCur.close();
        }
        return numbers;
    }


    public static long insertContact(Context context, String firstName, String lastName, String plugin, String user) {
        ContentValues values = new ContentValues();
        values.put(ContactsSQLiteHelper.FIRST_NAME, firstName);
        values.put(ContactsSQLiteHelper.LAST_NAME, lastName);
        values.put(ContactsSQLiteHelper.IMPORTED_PLUGIN, plugin);
        values.put(ContactsSQLiteHelper.IMPORTED_USER, user);
        SQLiteDatabase database = new ContactsSQLiteHelper(context).getWritableDatabase();
        return database.insert(ContactsSQLiteHelper.TABLE_NAME, null, values);
    }

    public static long insertNumber(Context context, long contactId, String number, int numberType) {
        ContentValues values = new ContentValues();
        values.put(NumbersSQLiteHelper.CONTACT_ID, contactId);
        values.put(NumbersSQLiteHelper.NUMBER, number);
        values.put(NumbersSQLiteHelper.NUMBER_TYPE, numberType);
        SQLiteDatabase database = new NumbersSQLiteHelper(context).getWritableDatabase();
        return database.insert(NumbersSQLiteHelper.TABLE_NAME, null, values);
    }

    public static long insertContact(Context applicationContext, String firstName, String lastName) {
        return insertContact(applicationContext, firstName, lastName, null, null);
    }
}
