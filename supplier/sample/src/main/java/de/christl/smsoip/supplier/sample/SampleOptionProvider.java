/*
 * Copyright (c) Danny Christl 2013.
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

package de.christl.smsoip.supplier.sample;

import android.graphics.drawable.Drawable;
import de.christl.smsoip.option.OptionProvider;

public class SampleOptionProvider extends OptionProvider {
    private static final String PROVIDER_NAME = "Sample";

    @Override
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
    }


    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
