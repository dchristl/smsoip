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

package de.christl.smsoip.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import de.christl.smsoip.R;
import de.christl.smsoip.database.DatabaseHandler;


public class IntentHandler {

    private Receiver givenReceiver;
    private String smsText;

    public IntentHandler(Intent intent, Context context) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        if (action.equals(Intent.ACTION_SENDTO)) {
            Uri data = intent.getData();
            if (data != null) {
                String number = data.getSchemeSpecificPart();
                givenReceiver = DatabaseHandler.findContactByNumber(number, context);
                if (givenReceiver == null) {
                    givenReceiver = new Receiver(context.getString(R.string.unknown));
                    givenReceiver.setRawNumber(number, context.getString(R.string.no_phone_type_label));
                }

            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object smsBody = extras.get("sms_body");
                if (smsBody != null && !smsBody.equals("")) {
                    smsText = smsBody.toString();
                }
            }
        } else if (action.equals(Intent.ACTION_SEND)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String type = intent.getType();
                StringBuilder builder = new StringBuilder();
                if (type != null && type.equals("text/plain")) {
                    builder.append(extras.getString(Intent.EXTRA_TEXT));

                } else if (type != null && type.equals("text/x-vcard")) {
                    Uri uri = (Uri) extras.get(Intent.EXTRA_STREAM);
                    builder.append(DatabaseHandler.resolveTextFileContent(uri, context));
                }
                smsText = builder.toString();
            }
        }
    }


    public Receiver getGivenReceiver() {
        return givenReceiver;
    }

    public String getSmsText() {
        return smsText;
    }
}

//        if (data != null) {
//            String givenReceiver;
//            ErrorReporterStack.put(LogConst.SET_PRESELECTED_CONTACT);
//            if (data.getScheme().equals(SMSReceiver.SMSOIP_SCHEME)) {
//                givenReceiver = data.getQueryParameter(SMSReceiver.NUMBER_PARAM);
//            } else {
//                givenReceiver = data.getSchemeSpecificPart();
//            }
//            if (givenReceiver != null && !givenReceiver.equals("")) {
//                Receiver contactByNumber = DatabaseHandler.findContactByNumber(givenReceiver, this);
//                if (contactByNumber == null) {
//                    contactByNumber = new Receiver(getString(R.string.unknown));
//                    contactByNumber.setRawNumber(givenReceiver, getString(R.string.no_phone_type_label));
//                }
//                addReceiver(contactByNumber);
//
//                SMSInputEditText smsInputEditText = (SMSInputEditText) findViewById(R.id.textInput);
//                //setting the cursor at the end
//                Bundle extras = intent.getExtras();
//                if (extras != null) {
//                    Object smsBody = extras.get("sms_body");
//                    if (smsBody != null && !smsBody.equals("")) {
//                        smsInputEditText.append(smsBody.toString());
//                    }
//                }
//                smsInputEditText.requestFocus();
//                smsInputEditText.processReplacement();
//            }
//        }