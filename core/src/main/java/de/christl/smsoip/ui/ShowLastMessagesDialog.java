package de.christl.smsoip.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
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
                Receiver receiver = receiverStringEntry.getKey();
                String message = receiverStringEntry.getValue();
                ((TextView) findViewById(R.id.contact)).setText(receiver.isUnknown() ? receiver.getRawNumber() : receiver.getName());
                ((TextView) findViewById(R.id.message)).setText(message);
            }
        }
        if (receiverList.isEmpty()) {
            findViewById(R.id.lastConversation).setVisibility(View.GONE);
        }
        LinearLayout layout = (LinearLayout) findViewById(R.id.conversationLayout);
        /* Create a new row to be added. */
        for (final Receiver receiver : receiverList) {
            LinkedList<Message> conversation = dbHandler.findConversation(receiver);
            TextView receiverView = new TextView(getContext());
            receiverView.setText(receiver.isUnknown() ? receiver.getRawNumber() : receiver.getName());
            receiverView.setTextColor(Color.parseColor("#C0F0C0"));
            receiverView.setPadding(5, 5, 5, 0);
            receiverView.setGravity(Gravity.CENTER);
            layout.addView(receiverView);

//            android:layout_width="wrap_content"
//            android:layout_height="wrap_content"
//            android:text="sampl esamplesamp lesam plesam plesamplesamp lesamplesample samplesamplesam plesamplesamplesamplesamplesa mplesamplesampl esamplesam plesample"
//            android:gravity="left"
//            android:paddingRight="50"
//            android:paddingLeft="20"
//            android:layout_gravity="center"
//            android:textColor="#F0EC6F"
            for (Message message : conversation) {
                TextView messageView = new TextView(getContext());
                messageView.setText(message.getMessage());
                if (message.isOutgoing()) {
                    messageView.setGravity(Gravity.RIGHT);
                    messageView.setPadding(50, 20, 20, 5);
                    messageView.setTextColor(Color.parseColor("#F0D64F"));
                } else {
                    messageView.setGravity(Gravity.LEFT);
                    messageView.setPadding(20, 20, 50, 5);
                    messageView.setTextColor(Color.parseColor("#F0EC6F"));
                }

                layout.addView(messageView);
            }
        }
    }
}
