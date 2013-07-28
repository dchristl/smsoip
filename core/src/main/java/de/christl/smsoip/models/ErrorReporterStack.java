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
import java.util.Collections;
import java.util.ConcurrentModificationException;

/**
 * abstract class for errorreporting aka action log
 */
public abstract class ErrorReporterStack {

    private static ArrayList<String> stack = new ArrayList<String>();

    public synchronized static void put(String action) {
        stack.add(action);
        try {
            if (stack.size() >= 10) {
                ArrayList<String> tmpList = new ArrayList<String>(stack);
                Collections.reverse(tmpList);
                int i = 0;
                for (String next : tmpList) {
                    if (i >= 10) {
                        stack.remove(next);
                    }
                    i++;
                }
            }
            updateErrorReport();
        } catch (ConcurrentModificationException e) {  //should not be s show stopper
            ACRA.getErrorReporter().handleSilentException(e);
        }
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
