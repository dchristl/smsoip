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
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.constant.TrackerConstants;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

@ReportsCrashes(/*formKey = "dGpSOGUxUHFabl9qUUc4NWdSNlBpZ3c6MQ",*/ mode = ReportingInteractionMode.NOTIFICATION,
        formKey = "",
        formUri = "https://smsoip.cloudant.com/acra-smsoip/_design/acra-storage/_update/report",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = "thereflifeepenturealcang",
        formUriBasicAuthPassword = "iwj0RjQheBUlWU845dpOprTQ",
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
    public static final String PLUGIN_ADFREE_PREFIX = "de.christl.smsoip.adfree";
    private HashMap<String, SMSoIPPlugin> loadedProviders = new HashMap<String, SMSoIPPlugin>();
    private HashMap<String, SMSoIPPlugin> notLoadedProviders = new HashMap<String, SMSoIPPlugin>();
    private HashMap<String, SMSoIPPlugin> pluginsToNew = new HashMap<String, SMSoIPPlugin>();
    private ArrayList<SMSoIPPlugin> plugins;
    private boolean writeToDatabaseAvailable = false;
    private boolean adsEnabled = true;
    private Integer installedPackages;
    private static Activity currentActivity;
    private boolean pickActionAvailable = true;
    private Exception appInitException;

    private Boolean isImagePickerAvailable;

    /**
     * helper for setting background to current activity
     *
     * @return
     */
    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * helper for setting background to current activity
     *
     * @return
     */
    public static void setCurrentActivity(Activity currentActivity) {
        SMSoIPApplication.currentActivity = currentActivity;
    }


    @Override
    /**
     * global entry point
     */
    public void onCreate() {
        ACRA.init(this);
        try {
            super.onCreate();
            app = this;
            setWriteToDBAvailable();
            initProviders();
            checkHash();
            checkForContactAvailability();
        } catch (Exception e) {
            //throw this exception later cause ACRA is not available here
            appInitException = e;
        }
    }

    public void throwlastExceptionIfAny() {
        if (appInitException != null) {
            ACRA.getErrorReporter().handleSilentException(appInitException);
            android.os.Process.killProcess(Process.myPid());
        }
    }

    /**
     * check if the pick contact activity is availabel on device
     */
    private void checkForContactAvailability() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        pickActionAvailable = getPackageManager().resolveActivity(pickIntent, 0) != null;
    }

    /**
     * check the hash for donate plugin
     */
    private void checkHash() {
        if (adsEnabled) {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String hash = defaultSharedPreferences.getString(SettingsConst.SERIAL, "");
            adsEnabled = !isHashValid(hash);
        }
    }

    /**
     * check if internal db is available
     */
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
                    ACRA.getErrorReporter().handleSilentException(e);
                }
            }
            readOutPlugins();
            installedPackages = installedApplications.size();
        }
    }

    /**
     * check tall plugins for validity
     *
     * @throws IOException
     * @throws IllegalAccessException
     */
    private void readOutPlugins() {
        //reset all lists
        loadedProviders.clear();
        pluginsToNew.clear();
        notLoadedProviders.clear();
        for (SMSoIPPlugin plugin : plugins) {
            String sourceDir = plugin.getSourceDir();
            DexFile apkDir;
            try {
                apkDir = new DexFile(sourceDir);
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                notLoadedProviders.put(plugin.getProviderName(), plugin);
                continue;
            }
            Enumeration<String> classFileEntries = apkDir.entries();

            //find all classes and save it in plugin
            while (classFileEntries.hasMoreElements()) {
                String s = classFileEntries.nextElement();
                if (!s.startsWith(PLUGIN_CLASS_PREFIX)) {
                    continue;
                }
                plugin.addAvailableClass(s);

            }
            Map<String, SMSoIPPlugin> currEntry = new HashMap<String, SMSoIPPlugin>();
            int minimalCoreVersion = -1;
            int loaded = 0;
            //iterate over all classes to find if its valid
            for (String s : plugin.getAvailableClasses()) {
                try {
                    Class<?> aClass = Class.forName(s, false, plugin.getPathClassLoader());
                    Class<?>[] aClassInterfaces = aClass.getInterfaces();
                    if (aClassInterfaces != null && ExtendedSMSSupplier.class.isAssignableFrom(aClass)) {

                        ExtendedSMSSupplier smsSupplier = (ExtendedSMSSupplier) aClass.newInstance();
                        plugin.setSupplier(smsSupplier);
                        currEntry.put(aClass.getCanonicalName(), plugin);
                        loaded++;

                    } else if (OptionProvider.class.isAssignableFrom(aClass)) { //found the optionprovider
                        Constructor<?> constructor = aClass.getConstructors()[0];  //get the first constructor and try to invoke it
                        OptionProvider provider;
                        if (constructor.getParameterTypes().length == 0) {
                            provider = (OptionProvider) aClass.newInstance();
                        } else {
                            provider = (OptionProvider) constructor.newInstance(((ExtendedSMSSupplier) null));
                        }
                        minimalCoreVersion = provider.getMinimalCoreVersion();
                        loaded++;
                    }
                } catch (ClassNotFoundException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                    notLoadedProviders.put(s, plugin);
                    break;
                } catch (InstantiationException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                    notLoadedProviders.put(s, plugin);
                    break;
                } catch (InvocationTargetException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                    notLoadedProviders.put(s, plugin);
                    break;
                } catch (IllegalAccessException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                    notLoadedProviders.put(s, plugin);
                    break;
                }
            }
            if (currEntry.size() == 1) {
                int versionCode = getVersionCode();
                for (Map.Entry<String, SMSoIPPlugin> stringSMSoIPPluginEntry : currEntry.entrySet()) {
                    if (loaded != 2) {  //exactly two classes (supplier and provider) should be loaded
                        notLoadedProviders.put(stringSMSoIPPluginEntry.getKey(), stringSMSoIPPluginEntry.getValue());
                    } else if (minimalCoreVersion <= versionCode) {
                        loadedProviders.put(stringSMSoIPPluginEntry.getKey(), stringSMSoIPPluginEntry.getValue());
                    } else {
                        pluginsToNew.put(stringSMSoIPPluginEntry.getKey(), stringSMSoIPPluginEntry.getValue());
                    }
                }
            }

        }
        buildAdditionalAcraInformations();
    }

    /**
     * build additonal infos for ACRA
     */
    private void buildAdditionalAcraInformations() {
        StringBuilder builder = new StringBuilder();
        for (SMSoIPPlugin smSoIPPlugin : loadedProviders.values()) {
            builder.append(smSoIPPlugin.getProviderName()).append(":");
            builder.append(" ").append(smSoIPPlugin.getVersion()).append("\n");
        }
        ACRA.getErrorReporter().putCustomData("PLUGINS", builder.toString());
        ACRA.getErrorReporter().putCustomData("STORE", getString(R.string.store_name));

    }

    /**
     * global pointer to app
     *
     * @return
     */
    public static SMSoIPApplication getApp() {
        return app;
    }

    /**
     * get all plugins
     *
     * @return
     */
    public Map<String, SMSoIPPlugin> getProviderEntries() {
        return loadedProviders;
    }

    /**
     * get plugin by name
     *
     * @param className
     * @return
     */
    public SMSoIPPlugin getSMSoIPPluginBySupplierName(String className) {
        return loadedProviders.get(className);
    }

    /**
     * get plugin by class name
     *
     * @param className
     * @return
     */
    private SMSoIPPlugin getPluginForClass(String className) {
        for (SMSoIPPlugin plugin : plugins) {
            if (plugin.isClassAvailable(className)) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * get plugin cant be loaded cause too old
     *
     * @return
     */
    public HashMap<String, SMSoIPPlugin> getNotLoadedProviders() {
        return notLoadedProviders;
    }

    /**
     * get plugin cant be loaded cause too new
     *
     * @return
     */
    public HashMap<String, SMSoIPPlugin> getPluginsToNew() {
        return pluginsToNew;
    }

    /**
     * is db availabele
     *
     * @return
     */
    public boolean isWriteToDatabaseAvailable() {
        return writeToDatabaseAvailable;
    }

    /**
     * checks if the imagePicker is available on device
     *
     * @return
     */
    public boolean isImagePickerAvailable() {
        if (isImagePickerAvailable == null) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            isImagePickerAvailable = getPackageManager().resolveActivity(photoPickerIntent, 0) != null;
        }
        return isImagePickerAvailable;
    }

    /**
     * is picking available
     *
     * @return
     */
    public boolean isPickActionAvailable() {
        return pickActionAvailable;
    }

    /**
     * load the text by resourceID from provider
     *
     * @param optionProvider
     * @param resourceId
     * @return
     */
    public String getTextByResourceId(OptionProvider optionProvider, int resourceId) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveStringResource(resourceId);
        }
        return getString(resourceId);
    }

    /**
     * get an array by resource id from provider
     *
     * @param optionProvider
     * @param resourceId
     * @return
     */
    public String[] getArrayByResourceId(OptionProvider optionProvider, int resourceId) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveArrayStringResource(resourceId);
        }
        return new String[]{"Resource not found"};
    }

    /**
     * get quantity string from provider
     *
     * @param optionProvider
     * @param resourceId
     * @param quantity
     * @return
     */
    public String getTextByResourceId(OptionProvider optionProvider, int resourceId, int quantity) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveStringResource(resourceId, quantity);
        }
        return getString(resourceId);
    }

    /**
     * advertisement should be loaded
     *
     * @return
     */
    public boolean isAdsEnabled() {
        return adsEnabled;
    }

    /**
     * get drawable from provider
     *
     * @param optionProvider
     * @param drawableId
     * @return
     */
    public Drawable getDrawable(OptionProvider optionProvider, int drawableId) {
        SMSoIPPlugin plugin = getPluginForClass(optionProvider.getClass().getCanonicalName());
        if (plugin != null) {
            return plugin.resolveDrawable(drawableId);
        }
        return null;
    }

    /**
     * get the current version code
     *
     * @return
     */
    public int getVersionCode() {
        try {
            return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return 9999;
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

    /**
     * get device id (for hash building)
     *
     * @return
     */
    public static String getDeviceId() {
        TelephonyManager telephonyManager = (TelephonyManager) app.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
    }

    /**
     * get an inputstream from provider
     *
     * @param optionProvider
     * @param resourceId
     * @return
     */
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

    /**
     * get a xml resource from provider
     *
     * @param optionProvider
     * @param xmlId
     * @return
     */
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
