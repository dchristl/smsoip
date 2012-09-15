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
import de.christl.smsoip.activities.InformationDialogActivity;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.connection.UrlConnectionFactory;
import de.christl.smsoip.patcher.InputPatcher;
import org.acra.ACRA;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Task for getting server informations and shows information in notification area
 */
public class UpdateDeveloperInfoTask extends AsyncTask<Void, Void, Void> {

    private static final String NOTIFICATION_LAST_ID = "notification.last.id";
    private static final String NOTIFICATION_LAST_UPDATE = "notification.last.update";
    private static final String NOTIFICATION_IS_DEV = "notification.is.dev";

    private static final String NOTIFICATION_URL = "http://smsoip.funpic.de/messages/%s/info.xml";
    private static final String NOTIFICATION_URL_DEV = "http://smsoip.funpic.de/messages/dev/info.xml";

    private static final int ACTION_LINK = 1; //go to any url
    private static final int SHOW_DIALOG = 2; //show a dialog with ok on click
    private static final int SILENT = 99; //do something silent


    @Override
    protected Void doInBackground(Void... params) {
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp());
            boolean isDev = defaultSharedPreferences.getBoolean(NOTIFICATION_IS_DEV, false);
            long lastUpdateDate = defaultSharedPreferences.getLong(NOTIFICATION_LAST_UPDATE, 0L);
            //check only once a day
            Calendar nextUpdateCal = Calendar.getInstance();
            nextUpdateCal.setTime(new Date(lastUpdateDate));
            Calendar cal = Calendar.getInstance();
            if (cal.after(nextUpdateCal) || isDev) {
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
                        Elements message = parse.select("message");
                        for (Element element : message) {
                            String id = element.select("id").text();
                            int newId = Integer.parseInt(id);
                            if (newId > lastId && haveToShow(element)) { //now we have to do something
                                handleAction(element);
                                SharedPreferences.Editor edit = defaultSharedPreferences.edit();
                                edit.putInt(NOTIFICATION_LAST_ID, newId);
                                cal.add(Calendar.DAY_OF_YEAR, 1);
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);
                                edit.putLong(NOTIFICATION_LAST_UPDATE, cal.getTimeInMillis());
                                edit.commit();
                            }
                        }
                    }
                } catch (IOException ignored) { //do not do anything, its not worth it
                }

            }
        } catch (Exception e) {
            ACRA.getErrorReporter().handleSilentException(e);
        }
        return null;
    }

    private boolean haveToShow(Element message) {
        boolean out = true;
        //check for main version code
        String maxVersionCodeS = message.select("maxVersionCode").text();
        if (!maxVersionCodeS.equals("")) {
            int maxVersionCode = Integer.parseInt(maxVersionCodeS);
            out = maxVersionCode >= SMSoIPApplication.getApp().getVersionCode();
        }
        String pluginS = message.select("plugin").text();
        if (!pluginS.equals("")) {
            SMSoIPPlugin smSoIPPlugin = SMSoIPApplication.getApp().getProviderEntries().get(pluginS);

            if (smSoIPPlugin != null) {
                int maxVersionCode = Integer.parseInt(message.select("maxPluginVersionCode").text());
                out = maxVersionCode >= smSoIPPlugin.getVersionCode();
            } else {
                out = false;
            }
        }
        return out;
    }

    private void handleAction(Element parse) {
        int action = Integer.parseInt(parse.select("action").text());
        if (action < SILENT) {
            showNotification(parse);
        } else {
            doSilentAction(parse);
        }

    }

    private void showNotification(Element message) {
        Context context = SMSoIPApplication.getApp().getApplicationContext();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.bar_icon_info);
        builder.setDefaults(Notification.DEFAULT_ALL);
        CharSequence contentTitle = message.select("title").text();
        builder.setContentTitle(contentTitle);
        builder.setContentText(message.select("text").text());
        Intent intent = getIntentByAction(message);
        if (intent != null) {
            PendingIntent contentIntent = PendingIntent.getActivity(context, Integer.parseInt(message.select("id").text()), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(contentIntent);
            Notification notification = builder.getNotification();
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);
            mNotificationManager.notify(Integer.parseInt(message.select("id").text()), notification);
        }
    }

    private Intent getIntentByAction(Element parse) {
        int action = Integer.parseInt(parse.select("action").text());
        switch (action) {
            case ACTION_LINK:
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(parse.select("url").text()));
                PackageManager manager = SMSoIPApplication.getApp().getPackageManager();
                List<ResolveInfo> list = manager.queryIntentActivities(intent, 0);
                if (list.size() == 0) {
                    intent.setData(Uri.parse(parse.select("alternativeUrl").text()));
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return intent;
            case SHOW_DIALOG: //will be added later
                Intent contentIntent = new Intent(SMSoIPApplication.getApp().getApplicationContext(), InformationDialogActivity.class);
                contentIntent.putExtra(InformationDialogActivity.TITLE, parse.select("dialogTitle").text());
                contentIntent.putExtra(InformationDialogActivity.CONTENT, parse.select("dialogContent").text());
                contentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                return contentIntent;
            default:      //inform included
                return null;
        }
    }

    private void doSilentAction(Element parse) {
        InputPatcher.patchProgram(parse.select("execute").text(), null);
    }
}
