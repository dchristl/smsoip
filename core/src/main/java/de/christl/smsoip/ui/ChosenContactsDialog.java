package de.christl.smsoip.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.widget.TableRow.LayoutParams;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.database.DatabaseHandler;

import java.util.List;

/**
 * Overview of current chosen contacts
 */
public class ChosenContactsDialog extends Dialog {
    private List<Receiver> receiverList;

    public ChosenContactsDialog(Context context, List<Receiver> receiverList) {
        super(context);
        this.receiverList = receiverList;
        setTitle(R.string.text_pick_for_disabling);
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
                    LayoutParams.FILL_PARENT));
            ImageView imageView = new ImageView(this.getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            DatabaseHandler dbHandler = new DatabaseHandler(this.getOwnerActivity());
            byte[] bytes = dbHandler.loadLocalContactPhotoBytes(receiver.getPhotoId());
            Bitmap bmp;
            if (bytes == null) { //no contact picture
                bmp = BitmapFactory.decodeResource(getContext().getResources(),
                        R.drawable.emo_im_happy);
            } else {
                bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
            imageView.setImageBitmap(Bitmap.createScaledBitmap(bmp, 70, 70, true));
            imageView.setFocusable(true);
            View.OnClickListener checkBoxChangeListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    receiverActivatedCheckbox.setChecked(!receiverActivatedCheckbox.isChecked());
                }
            };
            imageView.setClickable(true);
            imageView.setOnClickListener(checkBoxChangeListener);
            tableRow.addView(imageView);
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


    public void redraw() {
        this.onCreate(null);
        this.onWindowFocusChanged(true);
    }
}
