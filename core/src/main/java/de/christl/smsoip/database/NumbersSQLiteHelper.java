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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Class for handling the internal numbers database
 */
public class NumbersSQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "numbers";
    public static final String TABLE_NAME = DATABASE_NAME;
    private static final String CREATE_SCRIPT =
            "CREATE TABLE %s (\n" +
                    "  %s INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "  %s      TEXT    NOT NULL,\n" +
                    "  %s INTEGER NOT NULL,\n" +
                    "  %s INTEGER NOT NULL,\n" +
                    "  FOREIGN KEY (%s) REFERENCES %s (%s)\n" +
                    ");";

    public static final String _ID = "_id";
    public static final String NUMBER = "number";
    public static final String NUMBER_TYPE = "number_type";
    public static final String CONTACT_ID = "contact_id";
    public static final String[] ALL_COLUMNS = {_ID, NUMBER, NUMBER_TYPE, CONTACT_ID};

    public NumbersSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(CREATE_SCRIPT, TABLE_NAME, _ID, NUMBER, NUMBER_TYPE, CONTACT_ID, CONTACT_ID, TABLE_NAME, ContactsSQLiteHelper._ID));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //do nothing
    }
}
