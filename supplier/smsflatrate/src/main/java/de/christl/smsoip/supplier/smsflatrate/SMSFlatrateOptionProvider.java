/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.supplier.smsflatrate;

import android.graphics.drawable.Drawable;
import de.christl.smsoip.option.OptionProvider;

public class SMSFlatrateOptionProvider extends OptionProvider {

    public static final String PROVIDER_NAME = "SMSFlatrate";

    public SMSFlatrateOptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }


    @Override
    public int getMinimalCoreVersion() {
        return 46;
    }

    //    @Override
    public String getUserLabelText() {
        return getTextByResourceId(R.string.appkey_description);
    }

    //    @Override
    public String getPasswordLabelText() {
        return getTextByResourceId(R.string.appkey);
    }
}
