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

package de.christl.smsoip.activities.util;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.android.vending.billing.IInAppBillingService;

/**
 * Class for handling in app billing
 */
public class GooglePlayServiceConnection implements ServiceConnection {
    private IInAppBillingService playService;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        playService = IInAppBillingService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        playService = null;
    }
}
