package de.christl.smsoip.supplier.smsde;

import de.christl.smsoip.constant.Result;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SMSDeSupplierTest {
    private String USER_NAME;
    private String PASSWORD;
    private SMSDeSupplier supplier;

    @Before
    public void setUp() throws Exception {
        SMSDeOptionProvider provider = mock(SMSDeOptionProvider.class);
        supplier = new SMSDeSupplier(provider);
        //pass this values as -D options for executing test
        USER_NAME = System.getProperty("smsdeUser");
        PASSWORD = System.getProperty("smsdePass");
//        System.setProperty("http.proxyHost", "localhost");
//        System.setProperty("http.proxyPort", "8888");
    }

    @Test
    public void testLogin() throws Exception {
        assertEquals(supplier.login(USER_NAME, PASSWORD), Result.NO_ERROR());
        assertTrue(supplier.refreshInformationAfterMessageSuccessfulSent().getUserText().toString().contains(" Credits"));
//        assertEquals(supplier.fireSMSByText("Ha", new ArrayList<String>(), "asas"), Result.NO_ERROR());

    }


    @Test
    public void testCouldNotSent() throws Exception {
        Result result = supplier.processSendReturn(getClass().getResourceAsStream("bad.html"));
        assertEquals("Leider konnte Ihre Free-SMS nicht versendet werden.", result.getUserText());
    }

    @Test
    public void testSuccesfulSent() throws Exception {
        Result result = supplier.processSendReturn(getClass().getResourceAsStream("good.html"));
        assertEquals("OK! Ihre Free-SMS wurde erfolgreich versendet!", result.getUserText());
    }
}
