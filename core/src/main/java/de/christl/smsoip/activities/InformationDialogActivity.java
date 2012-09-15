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

package de.christl.smsoip.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.R;

/**
 * Class for showing a dialog if dialog information is clicked
 */
public class InformationDialogActivity extends Activity {

    public static final String TITLE = "title";
    public static String CONTENT = "content";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informationdialog);

        Bundle extras = getIntent().getExtras();
        setTitle(extras.getString(TITLE));
        TextView content = ((TextView) findViewById(R.id.content));
        String string = extras.getString(CONTENT);
        content.setText(Html.fromHtml(string));

        findViewById(R.id.closeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InformationDialogActivity.this.finish();
            }
        });
    }
}