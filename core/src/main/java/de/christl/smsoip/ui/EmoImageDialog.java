package de.christl.smsoip.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.constant.FireSMSResultList;

/**
 * since API level 14
 */
public class EmoImageDialog extends Dialog {
    private final FireSMSResultList.SendResult result;
    private final String resultMessage;

    public EmoImageDialog(Context context, FireSMSResultList.SendResult result, String resultMessage) {
        super(context);
        this.result = result;
        this.resultMessage = resultMessage;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagedialog);
        ImageView image = (ImageView) findViewById(R.id.image);
        switch (result) {
            case SUCCESS:
                image.setImageResource(R.drawable.ic_menu_happy);
                setTitle(getContext().getString(R.string.text_success));
                break;
            case ERROR:
                image.setImageResource(R.drawable.ic_menu_sad);
                setTitle(getContext().getString(R.string.text_error));
                break;
            default:
                image.setImageResource(R.drawable.ic_menu_schizophrenic);
                setTitle(getContext().getString(R.string.text_partial_success));
                break;
        }

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(resultMessage);

    }
}
