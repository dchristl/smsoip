package de.christl.smsoip.database;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.SendActivity;

import java.util.HashMap;
import java.util.Map;

/**
 * Handling class for all stuff for internal database
 */
public class DatabaseHandler {

    private SendActivity sendActivity;

    public DatabaseHandler(SendActivity sendActivity) {
        this.sendActivity = sendActivity;
    }

    public Receiver getPickedContactData(Uri contactData) {
        String pickedId = null;
        boolean hasPhone = false;
        String name = null;
        Receiver out;
        Cursor contactCur = sendActivity.managedQuery(contactData, null, null, null, null);
        if (contactCur.moveToFirst()) {
            pickedId = contactCur.getString(contactCur.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            name = contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            hasPhone = Integer.parseInt(contactCur.getString(contactCur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0;
        }
        out = new Receiver(pickedId, name);
        if (pickedId != null && hasPhone) {
            Cursor phones = sendActivity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
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
                String numberType = (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(sendActivity.getResources(), currEntry.getValue(), sendActivity.getText(R.string.text_no_phone_type_label));
                out.addNumber(currEntry.getKey(), numberType);
            }

        }
        return out;
    }

}
