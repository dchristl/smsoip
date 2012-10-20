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
import android.content.*;
import android.net.Uri;
import de.christl.smsoip.R;

public class AppRating {
    private final static String MARKET_URL = "market://details?id=de.christl.smsoip";
    private final static String ALTERNATIVE_URL = "https://play.google.com/store/apps/details?id=de.christl.smsoip";


    private final static int LAUNCHES_UNTIL_PROMPT = 20;
    public static final String LAUNCH_COUNT = "launch.count";
    public static final String RATING_DISABLED = "rating.disabled";

    public static void showRateDialogIfNeeded(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("rating.counter", 0);
        if (prefs.getBoolean(RATING_DISABLED, false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        int launchCount = prefs.getInt(LAUNCH_COUNT, 0) + 1;
        editor.putInt(LAUNCH_COUNT, launchCount);

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            showRateDialog(mContext, editor);
        }

        editor.commit();
    }

    public static void showRateDialog(final Context context, final SharedPreferences.Editor editor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rate_app_title);
        builder.setMessage(R.string.rate_app_message);

        builder.setPositiveButton(R.string.rate_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URL)));
                } catch (ActivityNotFoundException e) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ALTERNATIVE_URL)));
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.rate_no_thanks, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (editor != null) {
                    editor.putBoolean(RATING_DISABLED, true);
                    editor.commit();
                }
                dialog.dismiss();
            }
        });

        builder.setNeutralButton(R.string.rate_remind_me_later, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (editor != null) {
                    editor.remove(LAUNCH_COUNT);
                    editor.commit();
                }
                dialog.dismiss();
            }
        });

        builder.show();
    }
}