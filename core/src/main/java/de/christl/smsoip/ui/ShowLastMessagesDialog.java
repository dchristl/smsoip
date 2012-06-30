package de.christl.smsoip.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.database.DatabaseHandler;

import java.util.ArrayList;
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

        TableLayout tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        /* Create a new row to be added. */
        for (final Receiver receiver : receiverList) {
            TableRow receiverTableRow = new TableRow(getContext());
            TextView receiverView = new TextView(getContext());
            receiverView.setText(receiver.isUnknown() ? receiver.getRawNumber() : receiver.getName());
//            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
//                    TableRow.LayoutParams.WRAP_CONTENT);
//            layoutParams.span = 2;
//            receiverView.setLayoutParams(layoutParams);
            receiverView.setTextColor(Color.parseColor("#C0F0C0"));
            receiverView.setPadding(5, 5, 5, 5);
            receiverView.setGravity(Gravity.CENTER);

            receiverTableRow.addView(receiverView);
            tableLayout.addView(receiverTableRow);
            for (int i = 0; i < 5; i++) {
                TableRow messagTableRow = new TableRow(getContext());
                TextView messageView = new TextView(getContext());
                messageView.setText("Bla Blaasasasas");
                messageView.setGravity(i % 2 == 0 ? Gravity.LEFT : Gravity.RIGHT);
                messagTableRow.addView(messageView);
                tableLayout.addView(messagTableRow);
            }
        }
    }
}
