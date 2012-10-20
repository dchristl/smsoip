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
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.database.DatabaseHandler;
import de.christl.smsoip.models.Message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;

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

        Map<Receiver, String> lastMessage = DatabaseHandler.findLastMessage(appContext);
        String defaultUnknownText = appContext.getString(R.string.unknown);
        TextView lastIncomingMessage = (TextView) findViewById(R.id.message);
        if (lastMessage.size() > 0) {
            for (Map.Entry<Receiver, String> receiverStringEntry : lastMessage.entrySet()) {
                final Receiver receiver = receiverStringEntry.getKey();
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        receiverNumber = receiver.getRawNumber();
                        ShowLastMessagesDialog.this.dismiss();
                    }
                };
                String message = receiverStringEntry.getValue();
                TextView contact = (TextView) findViewById(R.id.contact);
                contact.setTypeface(null, Typeface.ITALIC);

                String name = receiver.getName();
                SpannableString content = new SpannableString(name.equals(defaultUnknownText) ? receiver.getRawNumber() : name);
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                contact.setText(content);
                contact.setOnClickListener(onClickListener);

                lastIncomingMessage.setText(message);
                lastIncomingMessage.setOnClickListener(onClickListener);
                findViewById(R.id.clickToAdd).setOnClickListener(onClickListener);
            }
        } else {
            findViewById(R.id.clickToAdd).setVisibility(View.GONE);
            lastIncomingMessage.setText(R.string.no_last_message);
        }
        if (receiverList.isEmpty()) {
            findViewById(R.id.lastConversation).setVisibility(View.GONE);
            return;
        }
        LinearLayout layout = (LinearLayout) findViewById(R.id.conversationLayout);
        /* Create a new row to be added. */
        for (final Receiver receiver : receiverList) {
            LinkedList<Message> conversation = DatabaseHandler.findConversation(receiver, appContext);
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
        }
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }
}
