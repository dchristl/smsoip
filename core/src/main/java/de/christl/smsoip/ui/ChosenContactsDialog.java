package de.christl.smsoip.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.*;
import android.widget.TableRow.LayoutParams;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;

import java.util.List;

/**
 * Overview of current chosen contacts
 */
public class ChosenContactsDialog extends Dialog {
    private List<Receiver> receiverList;

    public ChosenContactsDialog(Context context, List<Receiver> receiverList) {
        super(context);
        this.receiverList = receiverList;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chosencontactsdialog);
        TableLayout tableLayout = (TableLayout) findViewById(R.id.chosencontactstable);
        /* Create a new row to be added. */
        for (final Receiver receiver : receiverList) {

            final CheckBox receiverActivatedCheckbox = new CheckBox(this.getContext());
            receiverActivatedCheckbox.setChecked(receiver.isEnabled());
            receiverActivatedCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    receiver.setEnabled(isChecked);
                }
            });
            TableRow tableRow = new TableRow(this.getContext());
            tableRow.setLayoutParams(new LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));

            /* Add Button to row. */
            TextView name = new ContactsTextView(this.getContext(), receiverActivatedCheckbox);
            name.setText(receiver.getName());
            tableRow.addView(name);
            TextView number = new ContactsTextView(this.getContext(), receiverActivatedCheckbox);
            number.setText(receiver.getReceiverNumber());
            tableRow.addView(number);
            tableRow.addView(receiverActivatedCheckbox);
            /* Add row to TableLayout. */
            tableLayout.addView(tableRow, new TableLayout.LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));
        }

    }


}
