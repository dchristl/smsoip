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
import android.graphics.Typeface;
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
    private Context appContext;
    private ArrayList<Receiver> receiverList;
    private String receiverNumber = null;

    public ShowLastMessagesDialog(Context context, ArrayList<Receiver> receiverList) {
        super(context);
        this.appContext = context.getApplicationContext();
        this.receiverList = receiverList;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
                messageView.setTextColor(Color.parseColor("#0014F0"));
                messageView.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.message_left));
            } else {
                messageView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                messageView.setPadding(20, 5, 50, 20);
                messageView.setTextColor(Color.parseColor("#6400F0"));
                messageView.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.message_right));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                layoutParams.setMargins(0, 5, 50, 0);
                messageView.setLayoutParams(layoutParams);
            }

            layout.addView(messageView);
        }
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        if (defaultSharedPreferences.getBoolean(SettingsConst.CONVERSATION_ORDER, true)) {
            final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollbarlastMessagesTable);
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }
}
