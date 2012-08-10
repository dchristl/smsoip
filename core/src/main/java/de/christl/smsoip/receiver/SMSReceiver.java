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
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.settings.SMSReceiverPreference;
import de.christl.smsoip.database.DatabaseHandler;
import de.christl.smsoip.models.ErrorReporterStack;
import org.acra.ErrorReporter;

/**
 * Simple receiver to listen on incoming sms and shows notfication, with the possibility to start SMSoiP
 */
public class SMSReceiver extends BroadcastReceiver {

    public static int ID = 1;


    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences.getBoolean(SMSReceiverPreference.RECEIVER_ACTIVATED, true)) {  //is activated?

                ErrorReporterStack.put("message received by receiver");
                Bundle pudsBundle = intent.getExtras();
                Object[] pdus = (Object[]) pudsBundle.get("pdus");
                SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
                String ns = Context.NOTIFICATION_SERVICE;
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

                Notification notification = new Notification(R.drawable.bar_icon, messages.getMessageBody(), System.currentTimeMillis());
                notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND;
                CharSequence contentTitle = messages.getOriginatingAddress();
                Receiver contactByNumber = DatabaseHandler.findContactByNumber(messages.getOriginatingAddress(), context);
                if (contactByNumber != null) {
                    contentTitle = contactByNumber.getName();
                }
                CharSequence contentText = messages.getDisplayMessageBody();
                Uri inboxQuery = Uri.parse("smsoip:" + messages.getOriginatingAddress());
                Intent sendIntent = new Intent(Intent.ACTION_MAIN);
                sendIntent.setData(inboxQuery);
                sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);
                notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
                boolean onlyOneNotfctn = preferences.getBoolean(SMSReceiverPreference.RECEIVER_ONLY_ONE_NOTFICATION, false);
                int id = onlyOneNotfctn ? ID : ID++;
                mNotificationManager.notify(id, notification);
                if (preferences.getBoolean(SMSReceiverPreference.RECEIVER_ABORT_BROADCAST, false)) {
                    abortBroadcast();
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), "", e); //TODO remove after stability
            ErrorReporter.getInstance().handleSilentException(e);
        }
    }
}