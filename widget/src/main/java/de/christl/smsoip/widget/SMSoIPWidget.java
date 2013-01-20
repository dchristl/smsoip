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

package de.christl.smsoip.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * widget
 */
public class SMSoIPWidget extends AppWidgetProvider {

    public SMSoIPWidget() {
        super();

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("smsoip");
        builder.appendQueryParameter("provider", "de.christl.smsoip.supplier.goodmails.GoodmailsSupplier");
        Intent sendIntent = new Intent(Intent.ACTION_MAIN);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.setData(builder.build());
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.layout, contentIntent);
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.e("christl", "onEnabled = " + context);
    }


    @Override
    public IBinder peekService(Context myContext, Intent service) {
        return super.peekService(myContext, service);
    }
}
