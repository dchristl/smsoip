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

package de.christl.smsoip.activities.threading;

import android.os.AsyncTask;
import de.christl.smsoip.ui.BreakingProgressDialogFactory;

public class BreakingProgressAsyncTask<T> extends AsyncTask<BreakingProgressDialogFactory<T>, Void, T> {
    private BreakableTask<T> parentAsyncTask;

    public BreakingProgressAsyncTask(BreakableTask<T> parentAsyncTask) {
        this.parentAsyncTask = parentAsyncTask;
    }

    @Override
    protected T doInBackground(BreakingProgressDialogFactory<T>... params) {
        return params[0].getFutureResult();
    }


    @Override
    protected void onPostExecute(T smsActionResult) {
        parentAsyncTask.afterChildHasFinished(smsActionResult);
    }
}
