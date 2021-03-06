===Follow these simple steps to write your own plugin:===

 * Check out the sources
 * Create a subfolder of _supplier_ with the name of your plugin
 * Copy _pom.xml_ and _!AndroidManifest.xml_ of other plugin to the root of your new plugin
 * Make changes to the copied pom to your needs (like artifactId and version) and don't forget to add your new module to the supplier pom
 * Also change the !AndroidManifest (android:versionCode and package). The package name has to begin with *de.christl.smsoip.supplier.* otherwise it can't be found. The versionName attribute will be automatically replaced by maven build.
 * Create your new package under _src/main/java_
 * Create an !OptionProvider for your plugin and give your plugin a name, otherwise package name will be used:<br/>
 <code language="java">
 public class FreenetOptionProvider extends OptionProvider {
    private static String providerName = "Freenet";
    public FreenetOptionProvider() {
      super(providerName);
    }
 </code>
 * Create a supplier by implementing _SMSSupplier_ and implement all methods
 <code language="java">
 public class FreenetSupplier implements SMSSupplier {
     private FreenetOptionProvider provider;
     public FreenetSupplier() {
         provider = new FreenetOptionProvider();
     }
</code>
 * Have a look at the javadoc adn the [sequence diagrams](https://raw.githubusercontent.com/dchristl/smsoip/master/wiki/resources/SequenceDiagrams.pdf) for further informations
 * Please pack the code and send a mail with the attached file to me. Please use as subject: _Plugin SMSoIP_. You can find my mail address in the members area of this project. I will add it to app as soon as possible or contact you for commit rights to repository.