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
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.settings.SMSReceiverPreference;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.database.DatabaseHandler;
import de.christl.smsoip.models.ErrorReporterStack;
import org.acra.ACRA;

import java.io.FileNotFoundException;

/**
 * Simple receiver to listen on incoming sms and shows notfication, with the possibility to start SMSoiP
 */
public class SMSReceiver extends BroadcastReceiver {


    private static final String PDUS = "pdus";
    public static int ID = 1;


    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences.getBoolean(SMSReceiverPreference.RECEIVER_ACTIVATED, true)) {  //is activated?
                ErrorReporterStack.put(LogConst.MESSAGE_RECEIVED_BY_RECEIVER);
                Bundle pudsBundle = intent.getExtras();
                Object[] pdus = (Object[]) pudsBundle.get(PDUS);
                SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);


                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setAutoCancel(true);
                builder.setSmallIcon(R.drawable.bar_icon);
                String ringtoneUri = preferences.getString(SMSReceiverPreference.RECEIVER_RINGTONE_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
                builder.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

                if (ringtoneUri != null && !ringtoneUri.equals("")) {
                    Uri parse = Uri.parse(ringtoneUri);
                    try {
                        context.getContentResolver().openInputStream(parse);
                        builder.setSound(parse);
                    } catch (FileNotFoundException e) {
                        Log.e(this.getClass().getCanonicalName(), "", e);
                    }
                }
                CharSequence contentTitle = messages.getOriginatingAddress();
                Receiver contactByNumber = DatabaseHandler.findContactByNumber(messages.getOriginatingAddress(), context);
                if (contactByNumber != null) {
                    contentTitle = contactByNumber.getName();
                }
                builder.setContentTitle(contentTitle);

                builder.setContentText(messages.getMessageBody());

                Uri inboxQuery = Uri.parse("smsoip:" + messages.getOriginatingAddress());
                Intent sendIntent = new Intent(Intent.ACTION_MAIN);
                sendIntent.setData(inboxQuery);
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);
                builder.setContentIntent(contentIntent);
                Notification notification = builder.getNotification();
                String ns = Context.NOTIFICATION_SERVICE;
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
                boolean onlyOneNotfctn = preferences.getBoolean(SMSReceiverPreference.RECEIVER_ONLY_ONE_NOTFICATION, false);
                int id = onlyOneNotfctn ? ID : ID++;
                mNotificationManager.notify(id, notification);
            }
        } catch (Exception e) {
            //TODO remove after stability
            ACRA.getErrorReporter().handleSilentException(e);
        }
    }
}