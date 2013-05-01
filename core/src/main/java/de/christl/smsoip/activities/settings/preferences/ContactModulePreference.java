
/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.activities.settings.preferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.DialogPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.christl.smsoip.R;

import java.util.Map;

/**
 * Class for one contact preference
 */
public class ContactModulePreference extends DialogPreference {

    private final String key;
    private Context context;

    public ContactModulePreference(Context context, Map<String, String> textModules) {
        super(context, null);
        this.context = context;
        key = null;
        setNegativeButtonText(null);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.contact);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (key == null) {
            View inflate = LayoutInflater.from(context).inflate(R.layout.lastlistem, null);
            TextView viewById = (TextView) inflate.findViewById(R.id.addText);
            viewById.setText(R.string.add_contact);
            ColorStateList defaultColor = new TextView(getContext()).getTextColors();
            viewById.setTextColor(defaultColor);
            return inflate;
        } else {
            return super.onCreateView(parent);
        }
    }
}
