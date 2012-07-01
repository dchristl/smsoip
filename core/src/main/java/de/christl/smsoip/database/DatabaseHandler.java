package de.christl.smsoip.database;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.models.Message;

import java.util.*;

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
        Uri inboxQuery = Uri.parse("content://sms/inbox");    //only inbox will be queried
        Cursor cursor = parentActivity.getContentResolver().query(inboxQuery,
                new String[]{"address", "body"}, null, null, "date desc limit 1");
        parentActivity.startManagingCursor(cursor);
        String[] columns = new String[]{"address", "body"};
        if (cursor.getCount() > 0) {
            String count = Integer.toString(cursor.getCount());
            Log.e("Count", count);
            if (cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndex(columns[0]));
                String msg = cursor.getString(cursor.getColumnIndex(columns[1]));
                Receiver receiver = findContactByNumber(number);
                if (receiver == null) {
                    String text = (String) parentActivity.getText(R.string.text_unknown);
                    receiver = new Receiver("-1", text, 0);
                    receiver.addNumber(number, text);

                }
                String fixedNumber = receiver.getFixedNumberByRawNumber(number);
                receiver.setReceiverNumber(fixedNumber);
                out.put(receiver, msg);

            }
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
     * @return
     */
    public LinkedList<Message> findConversation(Receiver receiver) {
        String receiverNumber = receiver.getReceiverNumber();

        LinkedList<Message> out = new LinkedList<Message>();
        //query the outbox by rawnumber
        Uri smsQuery = Uri.parse("content://sms/");
        //replace all non numeric chars and get a number
        String selection = receiverNumber + "  like ('%' || replacedAddress) and type in (1,2)";
        String[] projection = {"cast(replace(replace(replace(replace(replace(address,'+',''),'-',''),')',''),'(',''),' ','') as int) as replacedAddress", "body", "date", "type"};
        Cursor cursor = parentActivity.getContentResolver().query(smsQuery,
                projection, selection, null, "date desc limit 10");
        while (cursor.moveToNext()) {
            String message = cursor.getString(1);
            Date date = new Date(cursor.getLong(2));
            int type = cursor.getInt(3);
            out.add(new Message(message, type == 2, date));
        }
        return out;
    }

    public void writeSMSInDatabase(ArrayList<Receiver> receiverList, String message) {
        for (Receiver receiver : receiverList) {
            ContentValues values = new ContentValues();
            values.put("address", receiver.getReceiverNumber());
            values.put("body", message);
            parentActivity.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
        }
    }
}
