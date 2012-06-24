package de.christl.smsoip.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Spanned;
import android.widget.ImageView;
import android.widget.TextView;
import de.christl.smsoip.R;


/**
 * Use the new EmoImageDialog instead
 */
@Deprecated
public class ImageDialog extends Dialog {
    private boolean success;
    private Spanned message;


    public ImageDialog(Context context, boolean success, Spanned message) {
        super(context);
        this.success = success;
        this.message = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagedialog);
        ImageView image = (ImageView) findViewById(R.id.image);
        if (success) {
            image.setImageResource(R.drawable.ic_menu_happy);
            setTitle(getContext().getString(R.string.text_success));
        } else {
            image.setImageResource(R.drawable.ic_menu_sad);
            setTitle(getContext().getString(R.string.text_error));
        }
        TextView text = (TextView) findViewById(R.id.text);
        text.setText(message);

    }

}
