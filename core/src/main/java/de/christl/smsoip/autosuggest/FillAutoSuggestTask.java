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

import android.os.AsyncTask;
import de.christl.smsoip.application.SMSoIPApplication;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * extra task for filling all stuff for autosuggest to improve startup of app
 */
public class FillAutoSuggestTask extends AsyncTask<Void, Void, List<NameNumberEntry>> {
    private WeakReference<NameNumberSuggestField> nameNumberSuggestFieldRef;

    public FillAutoSuggestTask(NameNumberSuggestField nameNumberSuggestFieldRef) {
        this.nameNumberSuggestFieldRef = new WeakReference<NameNumberSuggestField>(nameNumberSuggestFieldRef);
    }

    @Override
    protected List<NameNumberEntry> doInBackground(Void... params) {
        return ((SMSoIPApplication) nameNumberSuggestFieldRef.get().getContext().getApplicationContext()).getContactsWithPhoneNumberList();
    }

    @Override
    protected void onPostExecute(List<NameNumberEntry> nameNumberEntries) {
        if (nameNumberSuggestFieldRef != null) {
            nameNumberSuggestFieldRef.get().setAdapter(new NameNumberSuggestAdapter(nameNumberSuggestFieldRef.get().getContext(), nameNumberEntries));
            nameNumberSuggestFieldRef.get().setEnabled(true);
        }
    }
}
