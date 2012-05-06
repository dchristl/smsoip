package de.christl.smsoip.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import de.christl.smsoip.R;

/**
 * Created with IntelliJ IDEA.
 * User: Danny
 * Date: 06.05.12
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
public class SmallCheckBox extends CheckBox {
    public SmallCheckBox(Context context) {
        super(context);
//        setLayoutParams(new TableRow.LayoutParams(
//                TableRow.LayoutParams.WRAP_CONTENT,
//                TableRow.LayoutParams.WRAP_CONTENT));
//        setPadding(5, 1, 5, 1);
//        setButtonDrawable(R.drawable.checkbox);
//        setBackgroundColor(Color.BLUE);
//        setBackgroundDrawable(getResources().getDrawable(R.drawable.checkbox));
//        setButtonDrawable(R.drawable.checkbox);
    }

    public SmallCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        setButtonDrawable(R.drawable.checkbox);
    }
//    @Override
//    protected void drawableStateChanged() {
////        super.drawableStateChanged();
//        if (isChecked()) {
//            if (hasFocus()) {
//                setButtonDrawable(R.drawable.checkbox_on_background_focus_yellow);
//            } else {
//                setButtonDrawable(R.drawable.checkbox_on_background);
//
//            }
//        } else {
//            if (hasFocus()) {
//                setButtonDrawable(R.drawable.checkbox_off_background_focus_yellow);
//            } else {
//                setButtonDrawable(R.drawable.checkbox_off_background);
//            }
//        }
//
//        // Set the state of the Drawable
//    }

}
