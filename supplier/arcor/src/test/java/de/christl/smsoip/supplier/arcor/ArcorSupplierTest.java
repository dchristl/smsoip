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

package de.christl.smsoip.supplier.arcor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.InputStream;

import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(SMSoIPApplication.class)
public class ArcorSupplierTest {

    private ArcorSupplier supplier;
    private SMSoIPApplication app;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(SMSoIPApplication.class);
        supplier = mock(ArcorSupplier.class);
        Whitebox.setInternalState(supplier, "provider", mock(OptionProvider.class));
        app = mock(SMSoIPApplication.class);
        when(SMSoIPApplication.getApp()).thenReturn(app);

    }

    @Test
    public void testParseBalanceAvailable() throws Exception {

        InputStream resourceAsStream = ArcorSupplierTest.class.getResourceAsStream("balance.html");
        PowerMockito.doCallRealMethod().when(supplier).parseBalanceResponse(resourceAsStream);
        when(app.getTextByResourceId(any(OptionProvider.class), anyInt())).thenReturn("%1$s FreeSMS\n%2$s Bought");
        SMSActionResult smsActionResult = supplier.parseBalanceResponse(resourceAsStream);
        assertEquals("3 FreeSMS\n0 Bought", smsActionResult.getMessage());

    }

    @Test
    public void testParseNoBalanceAvailable() throws Exception {
        InputStream resourceAsStream = ArcorSupplierTest.class.getResourceAsStream("no_balance.html");
        PowerMockito.doCallRealMethod().when(supplier).parseBalanceResponse(resourceAsStream);
        when(app.getTextByResourceId(any(OptionProvider.class), anyInt())).thenReturn("%1$s FreeSMS\n%2$s Bought");
        SMSActionResult smsActionResult = supplier.parseBalanceResponse(resourceAsStream);
        assertEquals("Sie haben derzeit keine SMS zur Verfügung!", smsActionResult.getMessage());

    }


    @Test
    public void testParseUnlimitedBalance() throws Exception {
        InputStream resourceAsStream = ArcorSupplierTest.class.getResourceAsStream("balance_unlimited.html");
        PowerMockito.doCallRealMethod().when(supplier).parseBalanceResponse(resourceAsStream);
        when(app.getTextByResourceId(any(OptionProvider.class), anyInt())).thenReturn("%1$s FreeSMS\n%2$s Bought");
        SMSActionResult smsActionResult = supplier.parseBalanceResponse(resourceAsStream);
        assertEquals("unbegrenzt viele", smsActionResult.getMessage());

    }

    @Test
    public void testParseSendResponseSuccess() throws Exception {
        InputStream resourceAsStream = ArcorSupplierTest.class.getResourceAsStream("send_success.html");
        PowerMockito.doCallRealMethod().when(supplier).parseSendResponse(resourceAsStream);
        SMSActionResult smsActionResult = supplier.parseSendResponse(resourceAsStream);
        assertEquals("Es wurde 1 SMS an die Empfängernummer (+4564546465) gesendet! Die SMS wurde im Gesendet-Ordner gespeichert!", smsActionResult.getMessage());

    }

    @Test
    public void testParseSendResponseErrorNoCredits() throws Exception {
        InputStream resourceAsStream = ArcorSupplierTest.class.getResourceAsStream("send_error_no_credits.html");
        PowerMockito.doCallRealMethod().when(supplier).parseSendResponse(resourceAsStream);
        SMSActionResult smsActionResult = supplier.parseSendResponse(resourceAsStream);
        assertEquals("Sie haben derzeit keine SMS zur Verfügung.", smsActionResult.getMessage());

    }
}
