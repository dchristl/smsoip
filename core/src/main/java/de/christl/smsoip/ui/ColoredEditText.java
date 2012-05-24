package de.christl.smsoip.ui;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import de.christl.smsoip.activities.SendActivity;

/**
 * Class for marking text automatically in different colors
 */
public class ColoredEditText extends EditText {

    private int messageLength;

    public ColoredEditText(Context context) {
        this((SendActivity) context, null, 0);
    }

    public ColoredEditText(Context context, AttributeSet attrs) {
        this((SendActivity) context, attrs, 0);
    }

    public ColoredEditText(final SendActivity context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ((SendActivity) getContext()).updateSMScounter(charSequence);
            }

            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void updateColorsAndSMSCounter() {
        String lastText = getText().toString();

        String newText = lastText;
        int selectStart = this.getSelectionStart();
        int selectEnd = this.getSelectionEnd();
        if (lastText.length() > messageLength) {
            String uncolored = lastText.substring(0, messageLength);
            String colored = lastText.substring(messageLength);
            newText = uncolored + "<font color=\"" + Color.BLUE + "\">" + colored
                    + "</font>";
        }
        this.setText(Html.fromHtml(newText));

        try {
            this.setSelection(selectStart, selectEnd);
        } catch (IndexOutOfBoundsException e) {  //if the users is veeery fast, only for insurance
            Log.e(this.getClass().getCanonicalName(), "", e);
        }
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
        ((SendActivity) getContext()).updateSMScounter(getText());
//        updateColorsAndSMSCounter(getText());
    }
}
