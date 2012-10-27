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

package de.christl.smsoip.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import de.christl.smsoip.R;

/**
 * sending message dialog
 */
public class SendMessageDialog extends Dialog {
    public SendMessageDialog(Context context) {
        super(context);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagedialog);
        setCancelable(false);
        setTitle(R.string.smscomitted);
        ImageView image = (ImageView) findViewById(R.id.image);
        image.setImageResource(R.drawable.ic_menu_send);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            int width = getWindow().getDecorView().getWidth();
            Animation animation = new TranslateAnimation(0, width, 0, 0);
            animation.setDuration(2000);
            animation.setRepeatCount(-1);
            findViewById(R.id.image).setAnimation(animation);
        }
    }

}
