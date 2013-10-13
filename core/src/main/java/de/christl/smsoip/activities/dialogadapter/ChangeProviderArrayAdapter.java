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

package de.christl.smsoip.activities.dialogadapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;

/**
 * setting a custom layout for changing provider
 */
public class ChangeProviderArrayAdapter extends ArrayAdapter<SMSoIPPlugin> {

    private LinkedList<SMSoIPPlugin> filteredProviderEntries;
    private boolean cancelable;

    public ChangeProviderArrayAdapter(Context context, SMSoIPPlugin smSoIPPlugin) {
        super(context, 0);
        List<SMSoIPPlugin> providerEntries = SMSoIPApplication.getApp().getPlugins();
        filteredProviderEntries = new LinkedList<SMSoIPPlugin>();
        if (smSoIPPlugin == null) {   //add all if current provider not set
            filteredProviderEntries.addAll(providerEntries);
        } else {
            for (SMSoIPPlugin providerEntry : providerEntries) {     //filter out cause current provider should not be shown
                if (!providerEntry.getSupplierClassName().equals(smSoIPPlugin.getSupplierClassName())) {
                    filteredProviderEntries.add(providerEntry);
                }
            }
        }
        cancelable = providerEntries.size() != filteredProviderEntries.size();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater from = LayoutInflater.from(getContext());
        View inflate = from.inflate(R.layout.changeprovideradapter, null);
        TextView viewById = (TextView) inflate.findViewById(R.id.providerName);
        SMSoIPPlugin smSoIPPlugin = filteredProviderEntries.get(position);
        viewById.setText(smSoIPPlugin.getProviderName());
        Drawable drawable = smSoIPPlugin.getProvider().getIconDrawable();
        Drawable drawableLeft = null;
        Drawable drawableRight = null;
        if (position % 2 == 0) {
            drawableLeft = drawable;
        } else {
            drawableRight = drawable;

        }
        viewById.setCompoundDrawablesWithIntrinsicBounds(drawableLeft, null, drawableRight, null);
        return inflate;
    }

    @Override
    public int getCount() {
        return filteredProviderEntries.size();
    }

    @Override
    public SMSoIPPlugin getItem(int position) {
        return filteredProviderEntries.get(position);
    }

    @Override
    public int getPosition(SMSoIPPlugin item) {
        int out = 0;
        int count = 0;
        for (SMSoIPPlugin filteredProviderEntry : filteredProviderEntries) {
            if (filteredProviderEntry.equals(item)) {
                out = count;
            }
            count++;
        }

        return out;
    }


    public boolean isCancelable() {
        return cancelable;
    }
}
