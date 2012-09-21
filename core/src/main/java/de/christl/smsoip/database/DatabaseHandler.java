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
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.models.Message;
import de.christl.smsoip.picker.DateTimeObject;
import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.util.*;

/**
 * Handling class for all stuff for internal database
 */
public abstract class DatabaseHandler {


    public static Contact getPickedContactData(Uri contactData, Context context) {
        String pickedId = null;
        boolean hasPhone = false;
        String name = null;
        Contact out;
        Cursor contactCur = context.getContentResolver().query(contactData, null, null, null, null);
        if (contactCur.moveToFirst()) {
            pickedId = contactCur.getString(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            name = contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            hasPhone = Integer.parseInt(contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
        }
        contactCur.close();
        out = new Contact(name);
        if (pickedId != null && hasPhone) {
            Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{pickedId}, null);
            HashMap<String, Integer> phoneNumber = new HashMap<String, Integer>();
            while (phones.moveToNext()) {
                phoneNumber.put(phones.getString(
                        phones.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER)), phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
            }
            phones.close();
            for (Map.Entry<String, Integer> currEntry : phoneNumber.entrySet()) {
                String numberType = translateTypeToString(context, currEntry.getValue());
                out.addNumber(currEntry.getKey(), numberType);
            }

        }
        return out;
    }

    public static String translateTypeToString(Context context, int value) {
        return (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), value, context.getText(R.string.text_no_phone_type_label));
    }


    public static byte[] loadLocalContactPhotoBytes(String receiverNumber, Context context) {
        ContentResolver cr = context.getContentResolver();
        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, findPhotoIdByNumber(receiverNumber, context));
        Cursor c = cr.query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);
        if (c.moveToFirst()) {
            return c.getBlob(0);
        }
        c.close();
        return null;
    }

    private static int findPhotoIdByNumber(String receiverNumber, Context context) {
        int out = 0;
        String[] projection = new String[]{ContactsContract.Contacts.PHOTO_ID};
        String encodedNumber = Uri.encode(receiverNumber);
        if (!encodedNumber.equals("")) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, encodedNumber);
            try {
                ContentResolver contentResolver = context.getContentResolver();
                Cursor query = contentResolver.query(uri, projection, null, null, null);
                if (query.moveToFirst()) {
                    out = query.getInt(query.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
                }
                query.close();
            } catch (IllegalArgumentException e) {
                ErrorReporter instance = ACRA.getErrorReporter();
                instance.putCustomData("uri", uri.toString());
                instance.putCustomData("projection", Arrays.toString(projection));

                instance.handleSilentException(e);
            }
        }
        return out;
    }

    public static Map<Receiver, String> findLastMessage(Context context) {
        Map<Receiver, String> out = new HashMap<Receiver, String>(1);
        Uri inboxQuery = Uri.parse("content://sms/inbox");    //only inbox will be queried
        Cursor cursor = context.getContentResolver().query(inboxQuery,
                new String[]{"address", "body"}, null, null, "date desc limit 1");
        String[] columns = new String[]{"address", "body"};
        if (cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndex(columns[0]));
                String msg = cursor.getString(cursor.getColumnIndex(columns[1]));
                Receiver receiver = findContactByNumber(number, context);
                if (receiver == null) {
                    String text = context.getString(R.string.text_unknown);
                    receiver = new Receiver(text);
                    receiver.setRawNumber(number, context.getString(R.string.text_unknown));

                }
                out.put(receiver, msg);

            }
        }
        cursor.close();
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

        LinkedList<Message> out = new LinkedList<Message>();
        //query the outbox by rawnumber
        Uri smsQuery = Uri.parse("content://sms/");
        //replace all non numeric chars and get a number
        String selection = receiverNumber + "  like ('%' || replacedAddress) and type in (1,2)";
        String[] projection = {"cast(replace(replace(replace(replace(replace(address,'+',''),'-',''),')',''),'(',''),' ','') as int) as replacedAddress", "body", "date", "type"};
        Cursor cursor = context.getContentResolver().query(smsQuery,
                projection, selection, null, "date desc limit 10");
        while (cursor.moveToNext()) {
            String message = cursor.getString(1);
            Date date = new Date(cursor.getLong(2));
            int type = cursor.getInt(3);
            out.add(new Message(message, type == 2, date));
        }
        cursor.close();
        return out;
    }

    public static void writeSMSInDatabase(List<Receiver> receiverList, String message, DateTimeObject time, Context context) {
        try {
            for (Receiver receiver : receiverList) {
                ContentValues values = new ContentValues();
                values.put("address", receiver.getReceiverNumber());
                values.put("body", message);
                if (time != null) {
                    values.put("date", time.getCalendar().getTime().getTime());
                }
                context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
            }
        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }


    public static Receiver findContactByNumber(String rawNumber, Context context) {
        Receiver out = null;
        String name;
        String[] projection = new String[]{
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID, ContactsContract.Contacts.PHOTO_ID};
        String encodedNumber = Uri.encode(rawNumber);
        if (!encodedNumber.equals("")) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, encodedNumber);
            try {
                ContentResolver contentResolver = context.getContentResolver();
                Cursor query = contentResolver.query(uri, projection, null, null, null);
                if (query.moveToFirst()) {
                    name = query.getString(query.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                    if (name == null || name.equals("")) {
                        name = context.getString(R.string.text_unknown);
                    }
                    out = new Receiver(name);
                    out.setRawNumber(rawNumber, context.getString(R.string.text_unknown));
                }
                query.close();
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
}