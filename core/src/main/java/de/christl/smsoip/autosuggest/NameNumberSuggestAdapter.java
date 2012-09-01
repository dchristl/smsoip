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

package de.christl.smsoip.autosuggest;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import de.christl.smsoip.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter/Model for the auto suggest field
 */
public class NameNumberSuggestAdapter extends ArrayAdapter<NameNumberEntry> {

    private List<NameNumberEntry> items;
    public List<NameNumberEntry> filteredItems = new ArrayList<NameNumberEntry>();
    private final Object lock = new Object();
    private Filter filter = new NumberNameFilter();


    public NameNumberSuggestAdapter(Context context, List<NameNumberEntry> items) {
        super(context, R.layout.namenumbersuggestitem);
        this.items = items;
    }


    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Override
    public NameNumberEntry getItem(int position) {
        return filteredItems.get(position);
    }

    @Override
    public int getPosition(NameNumberEntry item) {
        return filteredItems.indexOf(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    public List<NameNumberEntry> getFilteredItems() {
        return filteredItems;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.namenumbersuggestitem, null);
        }
        TextView nameType = (TextView) convertView.findViewById(R.id.nameType);

        NameNumberEntry currentItem = getItem(position);
        String dropDownRepresentation = currentItem.getDropDownRepresentation();
        SpannableString spanString = new SpannableString(dropDownRepresentation);
        spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
        nameType.setText(spanString);
        TextView viewById = (TextView) convertView.findViewById(R.id.number);
        viewById.setText(currentItem.getNumber());
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }


    private final class NumberNameFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (filteredItems == null) {
                synchronized (lock) { // Notice the declaration above
                    filteredItems = new ArrayList<NameNumberEntry>();
                }
            }
            FilterResults out = new FilterResults();
            List<NameNumberEntry> tmpItems = new ArrayList<NameNumberEntry>();

            if (constraint != null) {
                for (NameNumberEntry item : items) {
                    if (item.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        tmpItems.add(item);
                    } else if (item.getNumber().contains(constraint)) {
                        tmpItems.add(item);
                    }
                }
            }
            out.values = tmpItems;
            out.count = tmpItems.size();

            return out;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            synchronized (lock) {
                filteredItems = (List<NameNumberEntry>) results.values;
            }
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

    }
}
