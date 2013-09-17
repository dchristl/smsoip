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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import org.acra.ACRA;

import java.util.Map;

import de.christl.smsoip.R;
import de.christl.smsoip.autosuggest.NumberUtils;
import de.christl.smsoip.constant.TrackerConstants;
import de.christl.smsoip.database.AndroidInternalDatabaseHandler;
import de.christl.smsoip.receiver.SMSReceiver;
import de.christl.smsoip.receiver.TransparentActivity;


public class IntentHandler {

    public static final String EXTRA_PROVIDER = "provider";
    public static final String EXTRA_SMS_BODY = "sms_body";
    private Receiver givenReceiver;
    private String smsText;
    private String supplier;

    public IntentHandler(Intent intent, Context context) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        String storeName = context.getString(R.string.store_name);
        if (action.equals(Intent.ACTION_SENDTO)) {
            Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_ENTRY_POINT, Intent.ACTION_SENDTO + storeName, TrackerConstants.LABEL_POS, null).build();
            EasyTracker.getInstance(context).send(build);
            Uri data = intent.getData();
            if (data != null) {
                String number = data.getSchemeSpecificPart();
                if (number != null && !number.equals("")) {
                    findAndSetReceiver(context, number);
                }
            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object smsBody = extras.get(EXTRA_SMS_BODY);
                if (smsBody != null && !smsBody.equals("")) {
                    smsText = smsBody.toString();
                }
            }
        } else if (action.equals(Intent.ACTION_SEND)) {
            Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_ENTRY_POINT, Intent.ACTION_SEND + storeName, TrackerConstants.LABEL_POS, null).build();
            EasyTracker.getInstance(context).send(build);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String type = intent.getType();
                StringBuilder builder = new StringBuilder();
                if (type != null && type.equals("text/plain")) {
                    builder.append(extras.getString(Intent.EXTRA_TEXT));

                } else if (type != null && type.equals("text/x-vcard")) {
                    Uri uri = (Uri) extras.get(Intent.EXTRA_STREAM);
                    builder.append(AndroidInternalDatabaseHandler.resolveTextFileContent(uri, context));
                }
                smsText = builder.toString();
            }
        } else if (action.equals(Intent.ACTION_MAIN)) {
            Uri data = intent.getData();
            if (data != null) {
                String scheme = data.getScheme();
                Bundle extras = intent.getExtras();
                if (scheme != null && scheme.equals(SMSReceiver.SMSOIP_SCHEME) && extras != null) {
                    Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_ENTRY_POINT, Intent.ACTION_MAIN + storeName, SMSReceiver.SMSOIP_SCHEME, null).build();
                    EasyTracker.getInstance(context).send(build);
                    String number = extras.getString(TransparentActivity.SENDER_NUMBER);
                    if (number != null && !number.equals("")) {
                        findAndSetReceiver(context, number);
                    }

                    if (!data.isOpaque()) {
                        String provider = extras.getString(EXTRA_PROVIDER);
                        if (provider != null && !provider.equals("")) {
                            //removeIt
                            ACRA.getErrorReporter().putCustomData("called_directly_with_provider", provider);
                            supplier = provider;
                        }
                    }
                } else {
                    Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_ENTRY_POINT, Intent.ACTION_MAIN + storeName, TrackerConstants.EVENT_NORMAL, null).build();
                    EasyTracker.getInstance(context).send(build);
                }
            }

        }
    }

    private void findAndSetReceiver(Context context, String number) {
        number = NumberUtils.fixNumber(number);
        givenReceiver = AndroidInternalDatabaseHandler.findContactByNumber(number, context);
        if (givenReceiver == null) {
            givenReceiver = new Receiver(context.getString(R.string.unknown));
            givenReceiver.setRawNumber(number, context.getString(R.string.no_phone_type_label));
        }
    }


    public Receiver getGivenReceiver() {
        return givenReceiver;
    }

    public String getSmsText() {
        return smsText;
    }

    public String getSupplier() {
        return supplier;
    }
}

