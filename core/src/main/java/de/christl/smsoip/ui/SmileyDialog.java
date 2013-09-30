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
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.acra.ACRA;

import java.util.LinkedHashMap;
import java.util.Map;

import de.christl.smsoip.R;

/**
 * all smileys
 */
public class SmileyDialog extends Dialog {

    private boolean result = false;
    private static final Map<Integer, String> smileyMap = new LinkedHashMap<Integer, String>(12) {
        {
            put(R.drawable.emo_im_happy, ":-) ");
            put(R.drawable.emo_im_sad, ":-( ");
            put(R.drawable.emo_im_surprised, "=-O ");
            put(R.drawable.emo_im_tongue_sticking_out, ":-P ");
            put(R.drawable.emo_im_winking, ";-) ");
            put(R.drawable.emo_im_kissing, ":-* ");
            put(R.drawable.emo_im_crying, ":'( ");
            put(R.drawable.emo_im_foot_in_mouth, ":-! ");
            put(R.drawable.emo_im_laughing, ":-D ");
            put(R.drawable.emo_im_lips_are_sealed, ":-X ");
            put(R.drawable.emo_im_undecided, ":-\\ ");
            put(R.drawable.emo_im_yelling, ":O ");
            put(R.drawable.emo_im_angel, "O:-) ");
            put(R.drawable.emo_im_cool, "B-) ");
            put(R.drawable.emo_im_embarrassed, ":-[ ");
            put(R.drawable.emo_im_wtf, "o_O ");
        }
    };
    private String item;

    public SmileyDialog(Context context) {
        super(context);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smileydialog);
        LinearLayout firstRow = (LinearLayout) findViewById(R.id.firstRow);
        LinearLayout secondRow = (LinearLayout) findViewById(R.id.secondRow);
        LinearLayout thirdRow = (LinearLayout) findViewById(R.id.thirdRow);
        LinearLayout fourthRow = (LinearLayout) findViewById(R.id.fourthRow);
        int count = 0;
        for (final Integer key : smileyMap.keySet()) {
            ImageView child = new ImageView(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(15, 15, 15, 15);
            child.setLayoutParams(lp);
            child.setBackgroundResource(key);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        dismiss();
                    } catch (IllegalArgumentException e) {
                        ACRA.getErrorReporter().handleSilentException(e);
                    }
                    result = true;
                    item = smileyMap.get(key);
                }
            });
            if (count < 4) {
                firstRow.addView(child);
            } else if (count < 8) {
                secondRow.addView(child);
            } else if (count < 12) {
                thirdRow.addView(child);
            } else {
                fourthRow.addView(child);
            }
            count++;

        }
    }

    @Override
    public void show() {
        super.show();
        result = false;
        item = null;
    }

    public boolean isPositiveResult() {
        return result;
    }

    public String getItem() {
        return item;
    }
}
