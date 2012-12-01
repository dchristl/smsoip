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

package de.christl.smsoip.models;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * abstract class for errorreporting aka action log
 */
public abstract class ErrorReporterStack {
    private static ArrayList<String> stack = new ArrayList<String>();

    public static void put(String action) {
        stack.add(action);
        if (stack.size() > 30) {
            int i = 0;
            for (Iterator<String> iterator = stack.iterator(); iterator.hasNext(); ) {
                iterator.next();
                if (i > 10) {
                    iterator.remove();
                }
                i++;

            }
        }
        updateErrorReport();
    }

    private static void updateErrorReport() {
        int z = 0;
        int size = stack.size();
        for (int i = size; i > 0; i--) {
            ACRA.getErrorReporter().putCustomData("action_" + z, stack.get(i - 1));
            z++;
            if (z >= 10) {
                break;
            }
        }
    }
}
