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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import org.acra.ACRA;

import de.christl.smsoip.R;
import de.christl.smsoip.autosuggest.NumberUtils;
import de.christl.smsoip.database.AndroidInternalDatabaseHandler;
import de.christl.smsoip.receiver.util.NotificationUtil;

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
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        NumberAnswerDialog dialog = new NumberAnswerDialog(this, number, senderName, message);
//        builder.setTitle(senderName);
//        final String finalNumber = number;
//        builder.setMessage(message)
//                .setPositiveButton(R.string.answer, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        Intent sendIntent = NotificationUtil.getSchemeIntent(finalNumber);
//                        startActivity(sendIntent);
//
//                        try {
//                            dialog.dismiss();
//                        } catch (IllegalArgumentException e) {
//                            ACRA.getErrorReporter().handleSilentException(e);
//                        }
//
//                    }
//                });
//        AlertDialog dialog = builder.create();
//        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                TransparentActivity.this.finish();
//            }
//        });
//        byte[] bytes = AndroidInternalDatabaseHandler.loadLocalContactPhotoBytes(number, this);
//        Bitmap bmp;
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inInputShareable = true;
//        options.inPurgeable = true;
//        if (bytes == null) { //no contact picture
//            bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_contact_picture_2, options);
//        } else {
//            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//        }
//        int bmpResolution = NumberUtils.getBitmapResolution(this);
//        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, bmpResolution, bmpResolution, true);
//        imageView.setImageBitmap(scaledBitmap);
//        imageView.setFocusable(true);
        dialog.show();

    }


}