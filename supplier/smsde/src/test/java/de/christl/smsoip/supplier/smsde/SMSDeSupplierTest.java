package de.christl.smsoip.supplier.smsde;

import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.SMSActionResult;
import org.junit.Before;
import org.junit.Ignore;
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
        SMSoIPApplication application = mock(SMSoIPApplication.class);
//        when(application.getString(anyInt())).thenReturn("");
        supplier = new SMSDeSupplier(provider);
        //pass this values as -D options for executing test
        USER_NAME = System.getProperty("smsdeUser");
        PASSWORD = System.getProperty("smsdePass");
//        System.setProperty("http.proxyHost", "localhost");
//        System.setProperty("http.proxyPort", "8888");
    }

    @Test
    public void testLogin() throws Exception {
        assertEquals(supplier.checkCredentials(USER_NAME, PASSWORD), SMSActionResult.NO_ERROR());
        assertTrue(supplier.refreshInformationAfterMessageSuccessfulSent().getUserText().toString().contains(" Credits"));
//        assertEquals(supplier.fireSMSByText("Ha", new ArrayList<String>(), "asas"), Result.NO_ERROR());

    }


    @Test
    public void testCouldNotSent() throws Exception {
        SMSDeSendResult resultSMSDe = supplier.processSendReturn(getClass().getResourceAsStream("bad.html"));
        assertEquals("Leider konnte Ihre Free-SMS nicht versendet werden.", resultSMSDe.getMessage());
    }

    @Test
    public void testSuccesfulSent() throws Exception {
        SMSDeSendResult resultSMSDe = supplier.processSendReturn(getClass().getResourceAsStream("good.html"));
        assertEquals("OK! Ihre Free-SMS wurde erfolgreich versendet!", resultSMSDe.getMessage());
    }

    @Test
    public void testTooLessCredits() throws Exception {
        SMSDeSendResult resultSMSDe = supplier.processSendReturn(getClass().getResourceAsStream("too_less_credits.html"));
        assertEquals("Fehler! Leider verfgen Sie nicht ber gengend credits, um diese 1 SMS zu verschicken. Ihr Kontostand betrgt zur Zeit 10 Credits. um 1 SMS zu versenden, bentigen Sie aber 10 Credits.", resultSMSDe.getMessage()); //Junit does not like german special chars
    }

    @Test
    @Ignore("comment out String getDefaultText(int messageId) in SMSACtionResult do get this work")
    public void testRefreshInfo() throws Exception {
        SMSActionResult resultSMSDe = supplier.processRefreshInformations(getClass().getResourceAsStream("refreshInfo.html"));
        assertTrue(resultSMSDe.getMessage().endsWith("Credits"));
    }


}
