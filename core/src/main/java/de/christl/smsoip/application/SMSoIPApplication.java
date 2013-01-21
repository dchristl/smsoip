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

package de.christl.smsoip.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@ReportsCrashes(formKey = "dGpSOGUxUHFabl9qUUc4NWdSNlBpZ3c6MQ", mode = ReportingInteractionMode.NOTIFICATION,
        resToastText = R.string.crash_toast_text,
        resNotifTickerText = R.string.crash_notif_ticker_text,
        resNotifTitle = R.string.crash_notif_title,
        resNotifText = R.string.crash_notif_text,
        resDialogText = R.string.crash_dialog_text,
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class SMSoIPApplication extends Application {

    private static SMSoIPApplication app;
    public static final String PLUGIN_CLASS_PREFIX = "de.christl.smsoip.supplier";
    //removeIt
    public static final String SMSOIP_PACKAGE = "de.christl.smsoip";
    public static final String PLUGIN_ADFREE_PREFIX = "de.christl.smsoip.adfree";
    private HashMap<String, SMSoIPPlugin> loadedProviders = new HashMap<String, SMSoIPPlugin>();
    private HashMap<String, SMSoIPPlugin> pluginsToOld = new HashMap<String, SMSoIPPlugin>();
    private HashMap<String, SMSoIPPlugin> pluginsToNew = new HashMap<String, SMSoIPPlugin>();
    private ArrayList<SMSoIPPlugin> plugins;
    private boolean writeToDatabaseAvailable = false;
    private boolean adsEnabled = true;
    private Integer installedPackages;
    private static Activity currentActivity;
    private boolean pickActionAvailable = true;

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(Activity currentActivity) {
        SMSoIPApplication.currentActivity = currentActivity;
    }


    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
        app = this;
        setWriteToDBAvailable();
        initProviders();
        checkHash();
        checkForContactAvailability();
    }

    private void checkForContactAvailability() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        pickActionAvailable = getPackageManager().resolveActivity(pickIntent, 0) != null;
    }

    private void checkHash() {
        if (adsEnabled) {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String hash = defaultSharedPreferences.getString(SettingsConst.SERIAL, "");
            adsEnabled = !isHashValid(hash);
        }
    }


    private void setWriteToDBAvailable() {
        try {
            Uri sentUri = Uri.parse("content://sms/sent");
            String type = getContentResolver().getType(sentUri);
            //uri is available, so check for every column we use
            String[] projection = {"date", "body", "address"};
            //just compile to see if all columns are available
            getContentResolver().query(sentUri, projection, null, null, null);
            if (type != null) {
                writeToDatabaseAvailable = true;
            }
        } catch (IllegalArgumentException e) {
            writeToDatabaseAvailable = false;
        } catch (SQLiteException e) {
            writeToDatabaseAvailable = false;
        }
    }

    /**
     * read out all packages to find installed plugins
     */
    public synchronized void initProviders() {
        List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);
//refresh only if not yet done and if a new application is installed
        if (installedPackages == null || !installedPackages.equals(installedApplications.size())) {
            plugins = new ArrayList<SMSoIPPlugin>();
            for (ApplicationInfo installedApplication : installedApplications) {
                try {
                    if (installedApplication.processName.startsWith(PLUGIN_CLASS_PREFIX)) {
                        PackageInfo packageInfo = getPackageManager().getPackageInfo(installedApplication.packageName, 0);
                        plugins.add(new SMSoIPPlugin(installedApplication, packageInfo, new PathClassLoader(installedApplication.sourceDir, getClassLoader())));
                    } else if (installedApplication.processName.startsWith(PLUGIN_ADFREE_PREFIX)) {
                        PackageManager manager = getPackageManager();
                        if (manager.checkSignatures(getPackageName(), PLUGIN_ADFREE_PREFIX) == PackageManager.SIGNATURE_MATCH) {
                            adsEnabled = false;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
            try {
                readOutPlugins();
            } catch (IOException e) {
                ACRA.getErrorReporter().handleException(e);
            } catch (IllegalAccessException e) {
                ACRA.getErrorReporter().handleException(e);
            }
            installedPackages = installedApplications.size();
        }

    }

    private void readOutPlugins() throws IOException, IllegalAccessException {
        //reset all lists
        loadedProviders.clear();
        pluginsToNew.clear();
        pluginsToOld.clear();
        for (SMSoIPPlugin plugin : plugins) {
            String sourceDir = plugin.getSourceDir();
            DexFile apkDir = new DexFile(sourceDir);
            Enumeration<String> classFileEntries = apkDir.entries();

            //find all classes and save it in plugin
            while (classFileEntries.hasMoreElements()) {
                String s = classFileEntries.nextElement();
                if (!s.startsWith(SMSOIP_PACKAGE)) {
                    continue;
                }
                plugin.addAvailableClass(s);

            }

            //iterate over all classes to find if its valid
            for (String s : plugin.getAvailableClasses()) {
                try {
                    Class<?> aClass = Class.forName(s, false, plugin.getPathClassLoader());
                    Class<?>[] aClassInterfaces = aClass.getInterfaces();
                    if (aClassInterfaces != null && ExtendedSMSSupplier.class.isAssignableFrom(aClass)) {

                        ExtendedSMSSupplier smsSupplier = (ExtendedSMSSupplier) aClass.newInstance();
                        plugin.setSupplier(smsSupplier);
                        loadedProviders.put(aClass.getCanonicalName(), plugin);
                        break;
                    }
                } catch (ClassNotFoundException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                    pluginsToNew.put(s, plugin);
                } catch (InstantiationException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                    pluginsToOld.put(s, plugin);
                }
            }

        }
        buildAdditionalAcraInformations();
    }

    private void buildAdditionalAcraInformations() {
        StringBuilder builder = new StringBuilder();
        for (SMSoIPPlugin smSoIPPlugin : loadedProviders.values()) {
            builder.append(smSoIPPlugin.getProviderName()).append(":");
            builder.append(" ").append(smSoIPPlugin.getVersion()).append("\n");
        }
        ACRA.getErrorReporter().putCustomData("PLUGINS", builder.toString());
    }


    public static SMSoIPApplication getApp() {
        return app;
    }

    public Map<String, SMSoIPPlugin> getProviderEntries() {
        return loadedProviders;
    }


    public SMSoIPPlugin getSMSoIPPluginBySupplierName(String className) {
        return loadedProviders.get(className);
    }


    private SMSoIPPlugin getPluginForClass(String className) {
        for (SMSoIPPlugin plugin : plugins) {
            if (plugin.isClassAvailable(className)) {
                return plugin;
            }
        }
        return null;
    }

    public HashMap<String, SMSoIPPlugin> getPluginsToOld() {
        return pluginsToOld;
    }

    public HashMap<String, SMSoIPPlugin> getPluginsToNew() {
        return pluginsToNew;
    }

    public boolean isWriteToDatabaseAvailable() {
        return writeToDatabaseAvailable;
    }

    public boolean isPickActionAvailable() {
        return pickActionAvailable;
    }

    public String getTextByResourceId(OptionProvider optionProvider, int resourceId) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveStringResource(resourceId);
        }
        return getString(resourceId);
    }

    public String[] getArrayByResourceId(OptionProvider optionProvider, int resourceId) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveArrayStringResource(resourceId);
        }
        return new String[]{"Resource not found"};
    }

    public String getTextByResourceId(OptionProvider optionProvider, int resourceId, int quantity) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveStringResource(resourceId, quantity);
        }
        return getString(resourceId);
    }


    public boolean isAdsEnabled() {
        return adsEnabled;
    }

    public Drawable getDrawable(OptionProvider optionProvider, int drawableId) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveDrawable(drawableId);
        }
        return null;
    }

    public int getVersionCode() {
        try {
            return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return 0;
    }

    /**
     * simple hash validation with some kind of "salt"
     * this is security by obscurity, but who cares this is open source tool and disabling ads should be no problem
     * salt is because its not so obvious and lock out black hats ;)
     *
     * @param hash
     * @return
     */
    public static boolean isHashValid(String hash) {
        String imei = getDeviceId() + "SMSoIP";
        return hash.equals(Integer.toHexString(imei.hashCode()));
    }

    public static String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) app.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    public InputStream getRawResourceByResourceId(OptionProvider optionProvider, int resourceId) {
        String canonicalName = optionProvider.getClass().getCanonicalName();
        SMSoIPPlugin plugin = getPluginForClass(canonicalName);
        if (plugin != null) {
            return plugin.resolveRawResource(resourceId);
        } else {
            handleNotFoundResource(canonicalName);
        }
        return null;
    }


    //removeIt
    private void handleNotFoundResource(String canonicalName) {
        ErrorReporter errorReporter = ACRA.getErrorReporter();
        errorReporter.putCustomData("class_should_be_available", canonicalName);
        errorReporter.putCustomData("available_classes", buildStringOfAllAvailableClasses());
    }

    //removeIt
    private String buildStringOfAllAvailableClasses() {
        StringBuilder builder = new StringBuilder();
        for (SMSoIPPlugin plugin : plugins) {
            builder.append(plugin.getProviderName());
            Set<String> availableClasses = plugin.getAvailableClasses();
            for (String availableClass : availableClasses) {
                builder.append(availableClass);
            }

        }

        return builder.toString();
    }


    public XmlResourceParser getXMLResourceByResourceId(OptionProvider optionProvider, int xmlId) {
        String canonicalName = optionProvider.getClass().getCanonicalName();
        SMSoIPPlugin plugin = getPluginForClass(canonicalName);
        if (plugin != null) {
            return plugin.resolveXML(xmlId);
        } else {
            handleNotFoundResource(canonicalName);
        }
        return null;
    }
}
