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

package de.christl.smsoip.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import org.acra.ACRA;

import java.io.FileNotFoundException;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.database.AndroidInternalDatabaseHandler;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.receiver.util.NotificationUtil;

/**
 * Simple receiver to listen on incoming sms and shows notfication, with the possibility to start SMSoiP
 */
public class SMSReceiver extends BroadcastReceiver {


    private static final String PDUS = "pdus";
    public static int ID = 1;
    public static final String SMSOIP_SCHEME = "smsoip";


//    public static void faker(Context context) {
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
//        builder.setAutoCancel(true);
//        builder.setSmallIcon(R.drawable.bar_icon);
//        builder.setContentTitle("ontentTitle");
//        Uri.Builder uriBuilder = new Uri.Builder();
//        uriBuilder.scheme(SMSOIP_SCHEME);
//
//        Intent sendIntent = new Intent(context, TransparentActivity.class);
//        sendIntent.putExtra(TransparentActivity.SENDER_NUMBER, "038asd gshj gdh sfg hdsgfhds ghfghdjsgfhdsghfgdshfrgsdhfgdsfghdsf");
//        sendIntent.putExtra(TransparentActivity.MESSAGE, "asasas");
//        sendIntent.setData(uriBuilder.build());
//        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);
//        builder.setContentIntent(contentIntent);
//        Notification notification = builder.getNotification();
//        String ns = Context.NOTIFICATION_SERVICE;
//        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
//        mNotificationManager.notify(ID, notification);
//    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(SettingsConst.RECEIVER_ACTIVATED, true)) {  //is activated?
            try {
                ErrorReporterStack.put(LogConst.MESSAGE_RECEIVED_BY_RECEIVER);
                Bundle pudsBundle = intent.getExtras();
                Object[] pdus = (Object[]) pudsBundle.get(PDUS);

                //take the first message for getting address
                SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
                if (messages == null) { //no message available on device
                    return;
                }
                //loop over all to get the whole message
                StringBuilder content = new StringBuilder();
                for (Object pdu : pdus) {
                    SmsMessage fromPdu = SmsMessage.createFromPdu((byte[]) pdu);
                    if (fromPdu != null) {
                        content.append(fromPdu.getMessageBody());
                    }
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setAutoCancel(true);
                builder.setSmallIcon(R.drawable.bar_icon);
                String ringtoneUri = preferences.getString(SettingsConst.RECEIVER_RINGTONE_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());

                if (preferences.getBoolean(SettingsConst.RECEIVER_LED_ACTIVATED, true)) {
                    builder.setDefaults(Notification.DEFAULT_LIGHTS);
                }
                if (preferences.getBoolean(SettingsConst.RECEIVER_VIBRATE_ACTIVATED, true)) {
                    builder.setVibrate(new long[]{0, 500, 100, 500, 100, 500});
                }

                if (ringtoneUri != null && !ringtoneUri.equals("")) {
                    Uri parse = Uri.parse(ringtoneUri);
                    try {
                        context.getContentResolver().openInputStream(parse);
                        builder.setSound(parse);
                    } catch (FileNotFoundException e) {
                        Log.e(this.getClass().getCanonicalName(), "", e);
                    } catch (NullPointerException e) {   //this can occur if parcel throws a FileNotFoundException
                        Log.e(this.getClass().getCanonicalName(), "", e);
                    }
                }
                String contentTitle = messages.getOriginatingAddress();
                Receiver contactByNumber = AndroidInternalDatabaseHandler.findContactByNumber(contentTitle, context);
                if (contactByNumber != null) {
                    contentTitle = contactByNumber.getName();
                }
                builder.setContentTitle(contentTitle);

                builder.setContentText(content);

                Intent sendIntent;
                if (preferences.getBoolean(SettingsConst.RECEIVER_SHOW_DIALOG, true)) {
                    sendIntent = new Intent(context, TransparentActivity.class);
                    sendIntent.putExtra(TransparentActivity.SENDER_NAME, contentTitle);
                    sendIntent.putExtra(TransparentActivity.MESSAGE, content.toString());
                    sendIntent.putExtra(TransparentActivity.SENDER_NUMBER, messages.getOriginatingAddress());
                } else {
                    sendIntent = NotificationUtil.getSchemeIntent(messages.getOriginatingAddress());
                }
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);
                builder.setContentIntent(contentIntent);
                Notification notification = builder.getNotification();
                String ns = Context.NOTIFICATION_SERVICE;
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
                boolean onlyOneNotfctn = preferences.getBoolean(SettingsConst.RECEIVER_ONLY_ONE_NOTFICATION, false);
                int id = onlyOneNotfctn ? ID : ID++;
                try {
                    mNotificationManager.notify(id, notification);
                } catch (SecurityException e) {
                    builder.setDefaults(Notification.DEFAULT_LIGHTS);
                    mNotificationManager.notify(id, builder.getNotification());
                }
            } catch (Exception e) {
                ACRA.getErrorReporter().handleSilentException(e);//just for insurance to avoid other apps will not work properly anymore
            }
        }
    }
}