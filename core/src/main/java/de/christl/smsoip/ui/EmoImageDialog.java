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
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.constant.FireSMSResultList;

/**
 * since API level 14
 */
public class EmoImageDialog extends Dialog {
    private final FireSMSResultList.SendResult result;
    private final String resultMessage;

    public EmoImageDialog(Context context, FireSMSResultList fireSMSResults, String resultMessage) {
        super(context);
        this.result = fireSMSResults.getResult();
        this.resultMessage = resultMessage;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagedialog);
        ImageView image = (ImageView) findViewById(R.id.image);
        switch (result) {
            case SUCCESS:
                image.setImageResource(R.drawable.ic_menu_happy);
                setTitle(getContext().getString(R.string.text_success));
                break;
            case ERROR:
                image.setImageResource(R.drawable.ic_menu_sad);
                setTitle(getContext().getString(R.string.text_error));
                break;
            default:
                image.setImageResource(R.drawable.ic_menu_schizophrenic);
                setTitle(getContext().getString(R.string.text_partial_success));
                break;
        }

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(Html.fromHtml(resultMessage));

    }
}
