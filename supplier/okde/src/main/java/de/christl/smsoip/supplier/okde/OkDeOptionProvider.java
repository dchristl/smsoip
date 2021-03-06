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

package de.christl.smsoip.supplier.okde;

import android.graphics.drawable.Drawable;

import de.christl.smsoip.option.OptionProvider;

/**
 *
 */
public class OkDeOptionProvider extends OptionProvider {

    private static final String providerName = "OK.de";

    @Override
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public int getMinimalCoreVersion() {
        return 48;
    }


    @Override
    public int getMaxMessageCount() {
        return 1;
    }
}
