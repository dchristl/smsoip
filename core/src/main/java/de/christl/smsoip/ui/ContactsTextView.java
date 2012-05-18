package de.christl.smsoip.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.*;
import android.widget.CheckBox;
import android.widget.TableRow;
import android.widget.TextView;
import de.christl.smsoip.R;

/**
 * The view for number and name in contacts table
 */
public class ContactsTextView extends TextView {
    public ContactsTextView(Context context, final CheckBox cb) {
        super(context);
        setGravity(Gravity.CENTER);
        setTextSize(16);
        setPadding(5, 1, 5, 1);
        setBackgroundResource(R.drawable.tablecellcontent);
        setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.FILL_PARENT,
                TableRow.LayoutParams.FILL_PARENT));
        setWidth(getWidthBySize());
        setLines(1);
        setFocusable(true);
        setEllipsize(TextUtils.TruncateAt.END);
        View.OnClickListener checkBoxChangeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.setChecked(!cb.isChecked());
            }
        };
        setClickable(true);
        setOnClickListener(checkBoxChangeListener);
    }



    public int getWidthBySize() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return (int) (0.35 * display.getWidth());
    }
}
