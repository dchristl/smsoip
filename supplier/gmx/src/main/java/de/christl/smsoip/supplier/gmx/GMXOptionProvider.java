/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.supplier.gmx;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "GMX";
    public static final String PROVIDER_CHECKNOFREESMSAVAILABLE = "provider.checknofreesmsavailable";

    public GMXOptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
        CheckBoxPreference checkNoFreeSMSAvailable = new CheckBoxPreference(context);
        checkNoFreeSMSAvailable.setKey(PROVIDER_CHECKNOFREESMSAVAILABLE);
        checkNoFreeSMSAvailable.setTitle(getTextByResourceId(R.string.text_check_no_free_available));
        checkNoFreeSMSAvailable.setSummary(getTextByResourceId(R.string.text_check_no_free_available_long));
        out.add(checkNoFreeSMSAvailable);
        return out;
    }

    @Override
    public int getMaxMessageCount() {
        return 5;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 161) {
            return 0;  //will be claimed usual way
        } else if (textLength < 305) {
            return 2;
        } else {
            textLength -= 304;
            int smsCount = Math.round((textLength / 152));
            smsCount = textLength % 152 == 0 ? smsCount : smsCount + 1;
            return smsCount + 2;
        }
    }

}
