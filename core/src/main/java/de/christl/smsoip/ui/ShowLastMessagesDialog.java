package de.christl.smsoip.ui;

import android.app.Activity;
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
    private DatabaseHandler dbHandler;
    private ArrayList<Receiver> receiverList;
    private String receiverNumber = null;

    public ShowLastMessagesDialog(Context context, ArrayList<Receiver> receiverList) {
        super(context);
        this.receiverList = receiverList;
        dbHandler = new DatabaseHandler(((Activity) context));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.showlastmessagesdialog);

        Map<Receiver, String> lastMessage = dbHandler.findLastMessage();
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
                SpannableString content = new SpannableString(receiver.isUnknown() ? receiver.getRawNumber() : receiver.getName());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                contact.setText(content);
                contact.setOnClickListener(onClickListener);
                TextView messageView = (TextView) findViewById(R.id.message);
                messageView.setText(message);
                messageView.setOnClickListener(onClickListener);
                findViewById(R.id.clickToAdd).setOnClickListener(onClickListener);
            }
        } else {
            findViewById(R.id.clickToAdd).setVisibility(View.GONE);
        }
        if (receiverList.isEmpty()) {
            findViewById(R.id.lastConversation).setVisibility(View.GONE);
            return;
        }
        LinearLayout layout = (LinearLayout) findViewById(R.id.conversationLayout);
        /* Create a new row to be added. */
        for (final Receiver receiver : receiverList) {
            LinkedList<Message> conversation = dbHandler.findConversation(receiver);
            TextView receiverView = new TextView(getContext());
            receiverView.setTextColor(Color.parseColor("#C0F0C0"));
            receiverView.setPadding(5, 5, 5, 0);
            receiverView.setGravity(Gravity.CENTER);
            receiverView.setTypeface(null, Typeface.ITALIC);
            SpannableString content = new SpannableString(receiver.isUnknown() ? receiver.getRawNumber() : receiver.getName());
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            receiverView.setText(content);
            layout.addView(receiverView);

            for (Message message : conversation) {
                TextView messageView = new TextView(getContext());
                messageView.setText(message.getMessage());
                if (message.isOutgoing()) {
                    messageView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
                    messageView.setPadding(50, 0, 20, 20);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(50, 0, 0, 0);
                    messageView.setLayoutParams(layoutParams);
                    messageView.setTextColor(Color.parseColor("#0014F0"));
                    messageView.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.message_left));
                } else {
                    messageView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                    messageView.setPadding(20, 0, 50, 20);
                    messageView.setTextColor(Color.parseColor("#6400F0"));
                    messageView.setBackgroundDrawable(getContext().getResources().getDrawable(R.drawable.message_right));
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 50, 0);
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
