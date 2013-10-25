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

package de.christl.smsoip.receiver;

import android.app.Activity;
import android.os.Bundle;

/**
 * not visible activity showing up a dialog
 */
public class TransparentActivity extends Activity {
    public static final String SENDER_NUMBER = "sender_number";
    public static final String SENDER_NAME = "sender_name";
    public static final String MESSAGE = "message";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String senderName = "";
        String message = "";
        final Bundle extras = getIntent().getExtras();
        String number = "";
        if (extras != null) {
            number = extras.getString(SENDER_NUMBER);
            senderName = extras.getString(SENDER_NAME);
            message = extras.getString(MESSAGE);
        }
        NumberAnswerDialog dialog = new NumberAnswerDialog(this, number, senderName, message);

        dialog.show();

    }


}