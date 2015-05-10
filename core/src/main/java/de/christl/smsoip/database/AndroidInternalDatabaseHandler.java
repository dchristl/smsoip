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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.models.Message;
import de.christl.smsoip.picker.DateTimeObject;

/**
 * Handling class for all stuff for internal database
 */
public abstract class AndroidInternalDatabaseHandler {


    private AndroidInternalDatabaseHandler() {
    }

    /**
     * return all datas to the picked contact
     *
     * @param contactData
     * @param context
     * @return
     */
    public static Contact getPickedContactData(Uri contactData, Context context) {
        String pickedId = null;
        boolean hasPhone = false;
        String name = null;
        Contact out;
        Cursor contactCur = context.getContentResolver().query(contactData, null, null, null, null);
        if (contactCur != null && contactCur.moveToFirst()) {
            pickedId = contactCur.getString(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            name = contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            hasPhone = Integer.parseInt(contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
        }
        if (contactCur != null) {
            contactCur.close();
        }
        out = new Contact(name);
        if (pickedId != null && hasPhone) {
            Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{pickedId}, null);
            HashMap<String, Integer> phoneNumber = new HashMap<String, Integer>();
            while (phones != null && phones.moveToNext()) {
                phoneNumber.put(phones.getString(
                        phones.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER)), phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
            }
            if (phones != null) {
                phones.close();
            }
            for (Map.Entry<String, Integer> currEntry : phoneNumber.entrySet()) {
                String numberType = translateTypeToString(context, currEntry.getValue());
                out.addNumber(currEntry.getKey(), numberType);
            }

        }
        return out;
    }

    /**
     * convenience mthod for showing the type of the number in the gui
     *
     * @param context
     * @param value
     * @return
     */
    public static String translateTypeToString(Context context, int value) {
        return (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), value, context.getText(R.string.no_phone_type_label));
    }

    /**
     * load the corresponding photo of the contact if any
     *
     * @param receiverNumber
     * @param context
     * @return
     */
    public static byte[] loadLocalContactPhotoBytes(String receiverNumber, Context context) {
        ContentResolver cr = context.getContentResolver();
        int photoIdByNumber = findPhotoIdByNumber(receiverNumber, context);
        //number can be negative sometimes causing IllegalArgumentExceptionn
        if (photoIdByNumber <= 0) {
            return null;
        }
        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoIdByNumber);
        Cursor c = cr.query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);
        if (c != null && c.moveToFirst()) {
            return c.getBlob(0);
        }
        if (c != null) {
            c.close();
        }
        return null;
    }

    /**
     * find the photoid for the contact
     *
     * @param receiverNumber
     * @param context
     * @return
     */
    private static int findPhotoIdByNumber(String receiverNumber, Context context) {
        int out = 0;
        String[] projection = new String[]{ContactsContract.Contacts.PHOTO_ID};
        String encodedNumber = Uri.encode(receiverNumber);
        if (!encodedNumber.equals("")) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, encodedNumber);
            try {
                ContentResolver contentResolver = context.getContentResolver();
                Cursor query = contentResolver.query(uri, projection, null, null, null);
                if (query != null && query.moveToFirst()) {
                    out = query.getInt(query.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                }
                if (query != null) {
                    query.close();
                }
            } catch (IllegalArgumentException e) {
                ErrorReporter instance = ACRA.getErrorReporter();
                instance.putCustomData("uri", uri.toString());
                instance.putCustomData("projection", Arrays.toString(projection));

                instance.handleSilentException(e);
            }
        }
        return out;
    }

    /**
     * find the receiver of last incoming message
     *
     * @param context
     * @return
     */
    public static Receiver findLastMessageReceiver(Context context) {
        Receiver out = null;
        Uri inboxQuery = Uri.parse("content://sms/inbox");    //only inbox will be queried
        Cursor cursor = context.getContentResolver().query(inboxQuery,
                new String[]{"address"}, null, null, "date desc limit 1");
        String[] columns = new String[]{"address", "body"};
        if (cursor != null && cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndex(columns[0]));
                Receiver receiver = findContactByNumber(number, context);
                if (receiver == null) {
                    String text = context.getString(R.string.unknown);
                    receiver = new Receiver(text);
                    receiver.setRawNumber(number, context.getString(R.string.unknown));

                }
                out = receiver;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return out;
    }

    /**
     * returns the last 10 messages with this receiver (ingoing and outgoing) in correct time order
     * query contains kind of fuzzy logic where, cause the receiver number is not saved in same way always
     * so special chars like (,),-,+ will be removed and the result will be cast to an integer to avoid leading zeros
     * the result (replacedAdress in query) is for example 49171123456 and the receivernumber for comparison is always a
     * number with leading zeros and international format like 0049171123456, so the replaced number have to be a subquery of
     * the receiver number (49171123456 is at the end of 0049171123456)
     * its imho the only way to get this by one query, other way is to fetch all and compare them by java, but it can
     * be very slow on phones with much messages
     *
     * @param receiver
     * @param context
     * @return
     */
    public static LinkedList<Message> findConversation(Receiver receiver, Context context) {
        String receiverNumber = receiver.getReceiverNumber();
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String convCountS = defaultSharedPreferences.getString(SettingsConst.CONVERSATION_COUNT, "10");
        int convCount = 10;
        try {
            convCount = Integer.parseInt(convCountS);
        } catch (NumberFormatException ignored) {

        }
        LinkedList<Message> out = new LinkedList<Message>();
        //query the outbox by rawnumber
        Uri smsQuery = Uri.parse("content://sms/");
        //replace all non numeric chars and get a number
        String selection = receiverNumber + "  like ('%' || replacedAddress) and type in (1,2)";
        String[] projection = {"cast(replace(replace(replace(replace(replace(address,'+',''),'-',''),')',''),'(',''),' ','') as int) as replacedAddress", "body", "date", "type"};


        String limit = convCount > 0 ? " limit " + convCount : "";

        Cursor cursor = context.getContentResolver().query(smsQuery, projection, selection, null, "date desc" + limit);
        while (cursor != null && cursor.moveToNext()) {
            String message = cursor.getString(1);
            Date date = new Date(cursor.getLong(2));
            int type = cursor.getInt(3);
            out.add(new Message(message, type == 2, date));
        }
        if (cursor != null) {
            cursor.close();
        }
        if (SMSoIPApplication.getApp().isUseOwnDatabase()) {
            SQLiteDatabase messageDB = new MessagesSQLiteHelper(context).getReadableDatabase();
            String where = MessagesSQLiteHelper.NUMBER + "='" + receiverNumber + "'";
            cursor = messageDB.query(MessagesSQLiteHelper.TABLE_NAME, MessagesSQLiteHelper.ALL_COLUMNS, where, null, null, null, MessagesSQLiteHelper.DATE + " desc" + limit);
            while (cursor != null && cursor.moveToNext()) {
                String message = cursor.getString(2);
                out.add(new Message(message, true, getDateTime(cursor.getString(3))));
            }
            if (cursor != null) {
                cursor.close();
            }
            messageDB.close();
        }
        Collections.sort(out, new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
                return rhs.getDate().compareTo(lhs.getDate());//younger before
            }
        });
        if (defaultSharedPreferences.getBoolean(SettingsConst.CONVERSATION_ORDER, true)) {
            Collections.reverse(out);
        }
        return out;
    }

    /**
     * write the sent sms in the internal outbox
     *
     * @param receiverList
     * @param message
     * @param time
     * @param context
     */
    public static void writeSMSInDatabase(List<Receiver> receiverList, String message, DateTimeObject time, Context context) {
        try {
            for (Receiver receiver : receiverList) {
                ContentValues values = new ContentValues();
                values.put("address", receiver.getReceiverNumber());
                values.put("body", message);

                if (SMSoIPApplication.getApp().isUseOwnDatabase()) {
                    if (time != null) {
                        values.put("date", getDateTime(time.getCalendar().getTime()));
                    }
                    SQLiteDatabase messageDB = new MessagesSQLiteHelper(context).getWritableDatabase();
                    messageDB.insert(MessagesSQLiteHelper.TABLE_NAME, null, values);
                    messageDB.close();

                } else {
                    if (time != null) {
                        values.put("date", time.getCalendar().getTime().getTime());
                    }
                    context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
                }
            }
        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }

    /**
     * resolve the contact back by the number
     *
     * @param rawNumber
     * @param context
     * @return
     */
    public static Receiver findContactByNumber(String rawNumber, Context context) {
        Receiver out = null;
        String name;
        String[] projection = new String[]{
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID, ContactsContract.Contacts.PHOTO_ID};
        String encodedNumber = Uri.encode(rawNumber);
        if (!encodedNumber.equals("")) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, encodedNumber);
            ContentResolver contentResolver = null;
            try {
                contentResolver = context.getContentResolver();
                Cursor query = contentResolver.query(uri, projection, null, null, null);
                if (query != null && query.moveToFirst()) {
                    name = query.getString(query.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    if (name == null || name.equals("")) {
                        name = context.getString(R.string.unknown);
                    }
                    out = new Receiver(name);
                    out.setRawNumber(rawNumber, context.getString(R.string.unknown));
                }
                if (query != null) {
                    query.close();
                }
            } catch (IllegalArgumentException e) {
                ErrorReporter instance = ACRA.getErrorReporter();
                instance.putCustomData("uri", uri.toString());
                instance.putCustomData("projection", Arrays.toString(projection));
                instance.handleSilentException(e);
            } catch (NullPointerException e) {
                ErrorReporter instance = ACRA.getErrorReporter();
                instance.putCustomData("uri", uri.toString());
                instance.putCustomData("projection", Arrays.toString(projection));
                instance.putCustomData("contentResolver", String.valueOf(contentResolver));
                instance.handleSilentException(e);
            }
        }
        return out;
    }

    /**
     * get the cursor by search term
     * return is ordered by mobile
     *
     * @param context
     * @param searchTerm
     * @return
     */
    public static Cursor getDBCursor(Context context, CharSequence searchTerm) {
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE};
        String selection = searchTerm == null ? null : ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE '%" + searchTerm + "%' OR " + ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE '%" + searchTerm + "%'";

        String orderby = "CASE WHEN " + ContactsContract.CommonDataKinds.Phone.TYPE + " = " + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                + " THEN 0" +
                " ELSE 1" +
                "  END";

        return context.getContentResolver().query(uri, projection, selection, null, orderby);
    }

    /**
     * used for sharing to build the string of a contact
     *
     * @param uri
     * @param context
     * @return
     */
    public static StringBuilder resolveTextFileContent(Uri uri, Context context) {
        StringBuilder builder = new StringBuilder();
        if (uri == null || context == null) {
            return builder;
        }
        ContentResolver cr = context.getContentResolver();
        InputStream stream = null;
        int ch;
        try {
            stream = cr.openInputStream(uri);
            while ((ch = stream.read()) != -1) {
                builder.append((char) ch);
            }
        } catch (FileNotFoundException e) {
            return builder;
        } catch (IOException e) {
            return builder;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }


        return builder;
    }

    /**
     * mark the given message as read
     *
     * @param context
     * @param number
     */
    public static void findMessageAndMarkAsRead(Context context, String number) {
        if (number != null && !number.equals("")) {
            Uri inboxQuery = Uri.parse("content://sms/inbox");    //only inbox will be queried
            Cursor cursor = context.getContentResolver().query(inboxQuery, new String[]{"address"}, "read = 0 and address = " + number, null, "_id desc");
            String[] columns = new String[]{"_id", "address", "body"};
            long id = -1;
            while (cursor != null && cursor.moveToNext()) {
                id = cursor.getLong(cursor.getColumnIndex(columns[0]));

            }
            if (id != -1) {
                markMessageAsRead(context, id);
            }
        }
    }

    private static void markMessageAsRead(Context context, long id) {
        ContentValues values = new ContentValues();
        values.put("read", 1);

        context.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + id, null);
    }

    private static String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    private static Date getDateTime(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return new Date(0);
        }
    }
}