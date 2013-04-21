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
 * Class for handling the internal contacts database
 */
public class ContactsSQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "contacts";
    public static final String TABLE_NAME = DATABASE_NAME;
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String IMPORTED_PLUGIN = "imported_plugin";
    public static final String IMPORTED_USER = "imported_user";
    public static final String _ID = "_id";
    public static final String[] ALL_COLUMNS = {_ID, FIRST_NAME, LAST_NAME, IMPORTED_PLUGIN, IMPORTED_USER};
    private static final String CREATE_SCRIPT = "CREATE TABLE %s (\n" +
            "  %s INTEGER     PRIMARY KEY AUTOINCREMENT,\n" +
            "  %s      TEXT NOT NULL,\n" +
            "  %s       TEXT NOT NULL,\n" +
            "  %s TEXT,\n" +
            "  %s   TEXT\n" +
            ");";

    public ContactsSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(CREATE_SCRIPT, TABLE_NAME, _ID, FIRST_NAME, LAST_NAME, IMPORTED_PLUGIN, IMPORTED_USER));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //do nothing
    }
}
