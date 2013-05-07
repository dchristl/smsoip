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

package de.christl.smsoip.receiver.util;

import android.content.Intent;
import android.net.Uri;
import de.christl.smsoip.receiver.SMSReceiver;
import de.christl.smsoip.receiver.TransparentActivity;

/**
 * Class for building scheme
 */
public abstract class NotificationUtil {

    private NotificationUtil() {
    }

    public static Intent getSchemeIntent(String number) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(SMSReceiver.SMSOIP_SCHEME);
        Intent sendIntent = new Intent(Intent.ACTION_MAIN);
        sendIntent.putExtra(TransparentActivity.SENDER_NUMBER, number);
        sendIntent.setData(uriBuilder.build());
        return sendIntent;
    }
}
