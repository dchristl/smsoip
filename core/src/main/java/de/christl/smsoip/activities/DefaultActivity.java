package de.christl.smsoip.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import de.christl.smsoip.R;

/**
 *
 */
public class DefaultActivity extends AllActivity {
    private static final int MAIN_MENU = 10;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem mainMenu = menu.add(0, MAIN_MENU, 0, getString(R.string.text_mainmenu));
        mainMenu.setIcon(R.drawable.menubutton);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == MAIN_MENU) {
            Intent intent =
                    new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.PARAMETER, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return true;
    }
}
