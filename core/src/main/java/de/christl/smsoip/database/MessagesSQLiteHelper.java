package de.christl.smsoip.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by danny on 10.05.15.
 */
public class MessagesSQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "messages";
    public static final String TABLE_NAME = DATABASE_NAME;
    private static final String CREATE_SCRIPT =
            "CREATE TABLE %s (\n" +
                    "  %s INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "  %s      TEXT    NOT NULL,\n" +
                    "  %s TEXT NOT NULL,\n" +
                    "  %s DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    public static final String _ID = "_id";
    public static final String NUMBER = "address";
    public static final String MESSAGE = "body";
    public static final String DATE = "date";
    public static final String[] ALL_COLUMNS = {_ID, NUMBER, MESSAGE, DATE};

    public MessagesSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(String.format(CREATE_SCRIPT, TABLE_NAME, _ID, NUMBER, MESSAGE, DATE));
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
