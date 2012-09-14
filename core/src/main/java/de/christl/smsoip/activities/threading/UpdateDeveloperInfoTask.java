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

package de.christl.smsoip.activities.threading;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.connection.UrlConnectionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Task for getting server informations and shows information in notification area
 */
public class UpdateDeveloperInfoTask extends AsyncTask<Void, Void, Boolean> {

    private static final String NOTIFICATION_LAST_ID = "notification.last.id";
    private static final String NOTIFICATION_LAST_UPDATE = "notification.last.update";
    private static final String NOTIFICATION_IS_DEV = "notification.is.dev";

    private static final String NOTIFICATION_URL = "http://smsoip.funpic.de/messages/%s/info.xml";
    private static final String NOTIFICATION_URL_DEV = "http://smsoip.funpic.de/messages/dev/info.xml";

    private static final int ACTION_LINK = 1; //go to any url
    private static final int MARKET_LINK = 2; // go to market
    private static final int INFORM = 3; //just inform, but do nothing else
    private static final int SHOW_DIALOG = 4; //show a dialog with ok on click
    private static final int SILENT = 99; //do something silent


    @Override
    protected Boolean doInBackground(Void... params) {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp());
        boolean isDev = defaultSharedPreferences.getBoolean(NOTIFICATION_IS_DEV, true);//todo exchange
        long lastUpdateDate = defaultSharedPreferences.getLong(NOTIFICATION_LAST_UPDATE, 0L);
        //check only once a day
        long nextUpdateDate = lastUpdateDate + (1000 * 60 * 60 * 24);
        if (nextUpdateDate <= System.currentTimeMillis()) {
            int lastId = defaultSharedPreferences.getInt(NOTIFICATION_LAST_ID, 0);
            String url;
            if (Locale.getDefault().equals(Locale.GERMANY)) {
                url = String.format(NOTIFICATION_URL, "de");
            } else {
                url = String.format(NOTIFICATION_URL, "en");
            }
            if (isDev) {
                url = NOTIFICATION_URL_DEV;
            }

            UrlConnectionFactory factory = new UrlConnectionFactory(url, UrlConnectionFactory.METHOD_GET);
            try {
                InputStream inputStream = factory.create().getInputStream();
                if (inputStream != null) {
                    Document parse = Jsoup.parse(UrlConnectionFactory.inputStream2DebugString(inputStream), "", Parser.xmlParser());
                    String id = parse.select("id").text();
                    if (Integer.parseInt(id) > lastId) { //now we have to do something
                        handleAction(parse);

                        return true;
                    }
                }
            } catch (IOException ignored) { //do not do anything, its not worth it
            }

        }
        return false;
    }

    private void handleAction(Document parse) {
        int action = Integer.parseInt(parse.select("action").text());
        if (action < SILENT) {
            showNotification(parse);
        } else {
            doSilentAction(parse);
        }

    }

    private void showNotification(Document parse) {
        Context context = SMSoIPApplication.getApp().getApplicationContext();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.bar_icon_info);
        builder.setDefaults(Notification.DEFAULT_ALL);
        CharSequence contentTitle = parse.select("title").text();
        builder.setContentTitle(contentTitle);
        builder.setContentText(parse.select("text").text());
        Intent intent = getIntentByAction(parse);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

        builder.setContentIntent(contentIntent);
        Notification notification = builder.getNotification();
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
        mNotificationManager.notify(2, notification);
    }

    private Intent getIntentByAction(Document parse) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(parse.select("url").text()));
        PackageManager manager = SMSoIPApplication.getApp().getPackageManager();
        List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);
        if (list.size() == 0) {
            intent.setData(Uri.parse(parse.select("alternativeUrl").text()));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void doSilentAction(Document parse) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
