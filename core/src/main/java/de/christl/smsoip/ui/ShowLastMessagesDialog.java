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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.LinkedList;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.database.AndroidInternalDatabaseHandler;
import de.christl.smsoip.models.Message;

/**
 * Class for a popup show the last message and all last conversations with picked contact
 */
public class ShowLastMessagesDialog extends Dialog {
    public static final String OUTGOING_DEFAULT_TEXT_COLOR = "0014F0";
    public static final String INCOMING_DEFAULT_TEXT_COLOR = "6400F0";
    private final SharedPreferences defaultSharedPreferences;
    private Context appContext;
    private ArrayList<Receiver> receiverList;
    private String receiverNumber = null;

    public static final String INCOMING_DEFAULT_COLOR = "36f603";
    public static final String OUTGOING_DEFAULT_COLOR = "e9c400";

    public ShowLastMessagesDialog(Context context, ArrayList<Receiver> receiverList) {
        super(context);
        this.appContext = context.getApplicationContext();
        this.receiverList = receiverList;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showlastmessagesdialog);
        LinearLayout layout = (LinearLayout) findViewById(R.id.conversationLayout);

        if (receiverList.isEmpty()) { //no receiver is currently chosen
            findViewById(R.id.hintAddReceivers).setVisibility(View.VISIBLE);
            Receiver lastMessageReceiver = AndroidInternalDatabaseHandler.findLastMessageReceiver(appContext);
            if (lastMessageReceiver != null) {
                LinkedList<Message> conversation = AndroidInternalDatabaseHandler.findConversation(lastMessageReceiver, appContext);
                addConversationToLayout(layout, lastMessageReceiver, conversation, true);
            }
        } else {
            findViewById(R.id.hintAddReceivers).setVisibility(View.GONE); //user should check how it works
        /* Create a new row to be added. */
            for (final Receiver receiver : receiverList) {
                LinkedList<Message> conversation = AndroidInternalDatabaseHandler.findConversation(receiver, appContext);
                addConversationToLayout(layout, receiver, conversation, false);
            }
        }
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        if (defaultSharedPreferences.getBoolean(SettingsConst.CONVERSATION_ORDER, true) && receiverList.size() <= 1) {
            final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollbarlastMessagesTable);
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    private void addConversationToLayout(LinearLayout layout, final Receiver receiver, LinkedList<Message> conversation, boolean addClickHint) {
        String defaultUnknownText = appContext.getString(R.string.unknown);
        TextView receiverView = new TextView(appContext);
        receiverView.setTextColor(Color.parseColor("#C0F0C0"));
        receiverView.setPadding(5, 5, 5, 0);
        receiverView.setGravity(Gravity.CENTER);
        receiverView.setTypeface(null, Typeface.ITALIC);
        String name = receiver.getName();
        SpannableString content = new SpannableString(name.equals(defaultUnknownText) ? receiver.getRawNumber() : name);
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        receiverView.setText(content);
        layout.addView(receiverView);
        if (addClickHint) {
            View.OnClickListener onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    receiverNumber = receiver.getRawNumber();
                    try {
                        ShowLastMessagesDialog.this.dismiss();
                    } catch (IllegalArgumentException e) {
                        ACRA.getErrorReporter().handleSilentException(e);
                    }
                }
            };
            receiverView.setOnClickListener(onClickListener);

            TextView clickToAddHint = new TextView(this.getContext());
            clickToAddHint.setText(appContext.getText(R.string.clickToAdd));
            clickToAddHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            clickToAddHint.setGravity(Gravity.CENTER);
            clickToAddHint.setOnClickListener(onClickListener);
            layout.addView(clickToAddHint);
        }

        if (conversation.isEmpty()) {
            TextView messageView = new TextView(appContext);
            messageView.setText(R.string.no_conversation);
            messageView.setGravity(Gravity.CENTER);
            messageView.setTextColor(Color.parseColor("#6400F0"));
            messageView.setBackgroundColor(Color.parseColor("#AAE9C400"));
            messageView.setPadding(5, 5, 5, 5);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
            layoutParams.setMargins(0, 5, 0, 5);
            messageView.setLayoutParams(layoutParams);
            layout.addView(messageView);
        }
        for (Message message : conversation) {
            TextView messageView = new TextView(appContext);
            messageView.setText(message.getMessage());
            if (message.isOutgoing()) {
                messageView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                messageView.setPadding(50, 5, 20, 20);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                layoutParams.setMargins(50, 5, 0, 0);
                messageView.setLayoutParams(layoutParams);
                String outColor = defaultSharedPreferences.getString(SettingsConst.OUTGOING_TEXT_COLOR, OUTGOING_DEFAULT_COLOR);
                messageView.setTextColor(getColor(outColor, OUTGOING_DEFAULT_TEXT_COLOR));
                messageView.setBackgroundDrawable(getOutgoingDrawable());
            } else {
                messageView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                messageView.setPadding(20, 5, 50, 20);
                String inColor = defaultSharedPreferences.getString(SettingsConst.OUTGOING_TEXT_COLOR, INCOMING_DEFAULT_TEXT_COLOR);
                messageView.setTextColor(getColor(inColor, INCOMING_DEFAULT_TEXT_COLOR));
                messageView.setBackgroundDrawable(getIncomingDrawable());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                layoutParams.setMargins(0, 5, 50, 0);
                messageView.setLayoutParams(layoutParams);
            }

            layout.addView(messageView);
        }
    }

    private int getColor(String newColor, String defaultColor) {
        int out;
        try {
            out = Color.parseColor("#" + newColor);
        } catch (IllegalArgumentException e) {
            out = Color.parseColor("#" + defaultColor);
        }
        return out;
    }

    private Drawable changeColor(Drawable drawable, String newColor, String defaultColor) {
        if (defaultColor.equalsIgnoreCase(newColor)) {
            return drawable;
        }
        int color;
        try {
            color = Color.parseColor(newColor);
        } catch (IllegalArgumentException e) {
            return drawable;
        }
        Drawable mutate = drawable.mutate();
        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;

        ColorMatrix cm = new ColorMatrix(new float[]{
                // Change red channel
                r, 0, 0, 0, 0,
                // Change green channel
                0, g, 0, 0, 0,
                // Change blue channel
                0, 0, b, 0, 0,
                // Keep alpha channel
                0, 0, 0, 1, 0,
        });
        ColorMatrixColorFilter cf = new ColorMatrixColorFilter(cm);
        mutate.setColorFilter(cf);
        return mutate;
    }


    private Drawable getIncomingDrawable() {
        Drawable drawable = getContext().getResources().getDrawable(R.drawable.message_right);
        String incomingColor = defaultSharedPreferences.getString(SettingsConst.INCOMING_COLOR, INCOMING_DEFAULT_COLOR);
        return changeColor(drawable, "#" + incomingColor, "#" + INCOMING_DEFAULT_COLOR);
    }

    private Drawable getOutgoingDrawable() {
        Drawable drawable = getContext().getResources().getDrawable(R.drawable.message_left);
        String outgoingColor = defaultSharedPreferences.getString(SettingsConst.OUTGOING_COLOR, OUTGOING_DEFAULT_COLOR);
        return changeColor(drawable, "#" + outgoingColor, "#" + OUTGOING_DEFAULT_COLOR);
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }
}
