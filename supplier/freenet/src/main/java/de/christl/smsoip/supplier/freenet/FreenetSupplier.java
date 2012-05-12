package de.christl.smsoip.supplier.freenet;

import android.text.Editable;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.List;

/**
 * Class for handling sms by freenet
 */
public class FreenetSupplier implements SMSSupplier {

    private FreenetOptionProvider provider;

    public FreenetSupplier() {
        provider = new FreenetOptionProvider();
    }

    @Override
    public Result refreshInformationOnRefreshButtonPressed() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Result refreshInformationAfterMessageSuccessfulSent() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getProviderInfo() {
        return provider.getProviderName();
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }

    @Override
//    POST /portal/login.php HTTP/1.1
//    Host: auth.freenet.de
//    User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64; rv:11.0) Gecko/20100101 Firefox/11.0
//    Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//    Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//    Accept-Encoding: gzip, deflate
//    Connection: keep-alive
//    Referer: http://www.freenet.de/index.html?status=log1&cbi=logMail
//    Cookie: frn075ism=0; __utma=47578168.257585167.1336828468.1336828468.1336828468.1; __utmb=47578168.4.10.1336828468; __utmc=47578168; __utmz=47578168.1336828468.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); frn075login=%7B%22u%22%3A%22binaryoutlaw%22%2C%22sssl%22%3Afalse%7D
//    Content-Type: application/x-www-form-urlencoded
//    Content-Length: 216
//
//    cbi=logMail&callback=http%3A%2F%2Ftools.freenet.de%2Fmod_perl%2Flinker%2Ffreenet_startseite_loginkasten_mail%2Fwebmail.freenet.de%2Flogin%2Findex.html&username=USERNAME&passtext=Passwort&password=PASSWORD&x=0&y=0
    public Result login(String userName, String password) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Result fireSMS(Editable smsText, List<Editable> receivers, String spinnerText) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
