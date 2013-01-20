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

package de.christl.smsoip.supplier.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import de.christl.smsoip.activities.R;


public abstract class InfoDialogActivity extends Activity {

    public static final String SMSOIP_SCHEME = "smsoip";
    public static final String PROVIDER_EXTRA = "provider";

    public void onCreate(Bundle savedInstanceState) {
        setTheme(android.R.style.Theme_Dialog);
        super.onCreate(savedInstanceState);
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(SMSOIP_SCHEME);
        Intent sendIntent = new Intent(Intent.ACTION_MAIN);
        sendIntent.putExtra(PROVIDER_EXTRA, getSupplierName());
        sendIntent.setData(uriBuilder.build());
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(sendIntent);
            this.finish();
        } catch (ActivityNotFoundException e) {
            setTitle(R.string.core_not_installed_head);
            TextView editText = new TextView(this);
            editText.setText(R.string.core_not_installed);
            addContentView(editText, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            editText.setGravity(Gravity.CENTER);
        }

    }

    protected abstract String getSupplierName();
}