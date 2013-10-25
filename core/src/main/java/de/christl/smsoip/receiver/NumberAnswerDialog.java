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
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.acra.ACRA;

import de.christl.smsoip.R;
import de.christl.smsoip.autosuggest.NumberUtils;
import de.christl.smsoip.database.AndroidInternalDatabaseHandler;
import de.christl.smsoip.receiver.util.NotificationUtil;

/**
 *
 */
public class NumberAnswerDialog extends Dialog {
    private Activity context;
    private final String message;
    private final String senderName;
    private String number;

    public NumberAnswerDialog(Activity context, String number, String senderName, String message) {
        super(context);
        this.context = context;
        this.message = message;
        this.senderName = senderName == null ? number : senderName;
        this.number = number;
        setContentView(R.layout.numberanswerdialog);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView headline = (TextView) findViewById(R.id.headline);
        byte[] bytes = AndroidInternalDatabaseHandler.loadLocalContactPhotoBytes(number, getContext());
        Bitmap bmp;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inInputShareable = true;
        options.inPurgeable = true;
        if (bytes == null) { //no contact picture
            bmp = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_contact_picture_2, options);
        } else {
            bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        }
        int bmpResolution = NumberUtils.getBitmapResolution(context);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, bmpResolution, bmpResolution, true);
        headline.setCompoundDrawablesWithIntrinsicBounds(new BitmapDrawable(scaledBitmap), null, null, null);
        headline.setText(senderName);
        TextView messageV = (TextView) findViewById(R.id.message);
        messageV.setText(message);
        Button answer = (Button) findViewById(R.id.answer);
        answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = NotificationUtil.getSchemeIntent(number);
                context.startActivity(sendIntent);

                try {
                    dismiss();
                } catch (IllegalArgumentException e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                }

            }
        });
    }
}
