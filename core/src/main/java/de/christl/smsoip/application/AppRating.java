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

package de.christl.smsoip.application;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import org.acra.ACRA;

import java.util.Map;

import de.christl.smsoip.R;
import de.christl.smsoip.backup.BackupHelper;
import de.christl.smsoip.constant.TrackerConstants;

public class AppRating {


    private final static int LAUNCHES_UNTIL_PROMPT = 30;
    public static final String LAUNCH_COUNT = "launch.count";
    public static final String RATING_DISABLED = "rating.disabled";
    private Context context;
    private SharedPreferences prefs;

    public AppRating(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences("de.christl.smsoip.rating", 0);
    }

    public void showRateDialogIfNeeded() {

        if (prefs.getBoolean(RATING_DISABLED, false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        int launchCount = prefs.getInt(LAUNCH_COUNT, 0) + 1;
        editor.putInt(LAUNCH_COUNT, launchCount);

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            showRateDialog(editor);
        }

        editor.commit();
        BackupHelper.dataChanged();
    }

    private void showRateDialog(final SharedPreferences.Editor editor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rate_app_title);
        builder.setMessage(R.string.rate_app_message);
        builder.setPositiveButton(R.string.rate_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_MISC, TrackerConstants.EVENT_RATING, TrackerConstants.LABEL_POS, null).build();
                EasyTracker.getInstance(context).send(build);
                try {
                    String uri = context.getString(R.string.market_rate_url);
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                } catch (ActivityNotFoundException e) {
                    String uri = context.getString(R.string.market_alternative_rate_url);
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
                }
                if (editor != null) {
                    editor.putBoolean(RATING_DISABLED, true);
                    editor.commit();
                    BackupHelper.dataChanged();
                }
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
        });

        builder.setNegativeButton(R.string.rate_no_thanks, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_MISC, TrackerConstants.EVENT_RATING, TrackerConstants.LABEL_NEG, null).build();
                EasyTracker.getInstance(context).send(build);
                if (editor != null) {
                    editor.putBoolean(RATING_DISABLED, true);
                    editor.commit();
                    BackupHelper.dataChanged();
                }
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
        });

        builder.setNeutralButton(R.string.rate_remind_me_later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_MISC, TrackerConstants.EVENT_RATING, TrackerConstants.LABEL_CANCEL, null).build();
                EasyTracker.getInstance(context).send(build);
                if (editor != null) {
                    editor.remove(LAUNCH_COUNT);
                    editor.commit();
                    BackupHelper.dataChanged();
                }
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Map<String, String> build = MapBuilder.createEvent(TrackerConstants.CAT_MISC, TrackerConstants.EVENT_RATING, TrackerConstants.LABEL_CANCEL, null).build();
                EasyTracker.getInstance(context).send(build);
                if (editor != null) {
                    editor.remove(LAUNCH_COUNT);
                    editor.commit();
                    BackupHelper.dataChanged();
                }
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
        });
        builder.show();
    }
}