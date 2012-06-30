package de.christl.smsoip.database;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;

import java.util.HashMap;
import java.util.Map;

/**
 * Handling class for all stuff for internal database
 */
public class DatabaseHandler {

    private Activity parentActivity;

    public DatabaseHandler(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public Receiver getPickedContactData(Uri contactData) {
        String pickedId = null;
        boolean hasPhone = false;
        String name = null;
        Receiver out;
        Cursor contactCur = parentActivity.managedQuery(contactData, null, null, null, null);
        int photoId = 0;
        if (contactCur.moveToFirst()) {
            pickedId = contactCur.getString(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            name = contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            hasPhone = Integer.parseInt(contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
            photoId = contactCur.getInt(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_ID));
        }
        out = new Receiver(pickedId, name, photoId);
        if (pickedId != null && hasPhone) {
            Cursor phones = parentActivity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{pickedId}, null);
            HashMap<String, Integer> phoneNumber = new HashMap<String, Integer>();
            while (phones.moveToNext()) {
                phoneNumber.put(phones.getString(
                        phones.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER)), phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2)));
            }
            phones.close();
            for (Map.Entry<String, Integer> currEntry : phoneNumber.entrySet()) {
                String numberType = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(parentActivity.getResources(), currEntry.getValue(), parentActivity.getText(R.string.text_no_phone_type_label));
                out.addNumber(currEntry.getKey(), numberType);
            }

        }
        return out;
    }

    public Receiver findContactByNumber(String givenNumber) {
        Receiver out = null;
        String name;
        String[] projection = new String[]{
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID, ContactsContract.Contacts.PHOTO_ID};

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(givenNumber));
        Cursor query = parentActivity.getContentResolver().query(uri, projection, null, null, null);
        if (query.moveToFirst()) {
            name = query.getString(query.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            if (name == null || name.equals("")) {
                name = (String) parentActivity.getText(R.string.text_unknown);
            }
            String id = query.getString(query.getColumnIndex(ContactsContract.PhoneLookup._ID));
            if (id == null || id.equals("")) {
                id = "-1";
            }
            int photoId = query.getInt(query.getColumnIndex(ContactsContract.Contacts.PHOTO_ID));
            out = new Receiver(id, name, photoId);
            out.addNumber(givenNumber, "");
        }
        query.close();
        return out;
    }


    public byte[] loadLocalContactPhotoBytes(int photoId) {
        ContentResolver cr = parentActivity.getContentResolver();
        Uri photoUri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoId);
        Cursor c = cr.query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO}, null, null, null);
        if (c.moveToFirst()) {
            return c.getBlob(0);
        }
        return null;
    }

    public Map<Receiver, String> findLastMessage() {
        Map<Receiver, String> out = new HashMap<Receiver, String>(1);
        Uri mSmsinboxQueryUri = Uri.parse("content://sms/inbox");    //only inbox will be queried
        Cursor cursor = parentActivity.getContentResolver().query(mSmsinboxQueryUri,
                new String[]{"_id", "thread_id", "address", "person", "date",
                        "body", "type"}, null, null, "date desc limit 1");
        parentActivity.startManagingCursor(cursor);
        String[] columns = new String[]{"address", "person", "date", "body", "type"};
        if (cursor.getCount() > 0) {
            String count = Integer.toString(cursor.getCount());
            Log.e("Count", count);
            if (cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndex(columns[0]));
                String name = cursor.getString(cursor.getColumnIndex(columns[1]));
                String date = cursor.getString(cursor.getColumnIndex(columns[2]));
                String msg = cursor.getString(cursor.getColumnIndex(columns[3]));
                String type = cursor.getString(cursor.getColumnIndex(columns[4]));
                Log.e(parentActivity.getClass().getCanonicalName(), "number " + number);
                Log.e(parentActivity.getClass().getCanonicalName(), "name " + name);
                Log.e(parentActivity.getClass().getCanonicalName(), "date " + date);
                Log.e(parentActivity.getClass().getCanonicalName(), "msg " + msg);
                Log.e(parentActivity.getClass().getCanonicalName(), "type " + type);
                Receiver receiver = findContactByNumber(number);
                if (receiver == null) {
                    receiver = new Receiver("-1", (String) parentActivity.getText(R.string.text_unknown), 0);
                    receiver.addNumber(number);

                }
                Log.e(parentActivity.getClass().getCanonicalName(), "Receiver " + receiver);
                out.put(receiver, msg);

            }
        }
        return out;
    }

}
