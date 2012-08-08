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

package de.christl.smsoip.service.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * Simple receiver to listen on incoming sms and shows notfication, with the possibility to start SMSoiP
 */
public class SMSReceiver extends BroadcastReceiver {

    public static int ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle pudsBundle = intent.getExtras();
        Object[] pdus = (Object[]) pudsBundle.get("pdus");
        SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

        Notification notification = new Notification(R.drawable.icon, messages.getMessageBody(), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_SOUND;
        CharSequence contentTitle = String.format(context.getString(R.string.text_answer_directly), messages.getOriginatingAddress());
        CharSequence contentText = messages.getMessageBody();
        //TODO check if activity exists
        Uri inboxQuery = Uri.parse("smsoip:" + messages.getOriginatingAddress());
        Intent sendIntent = new Intent(Intent.ACTION_MAIN);
        sendIntent.setData(inboxQuery);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        mNotificationManager.notify(ID++, notification);
    }
}