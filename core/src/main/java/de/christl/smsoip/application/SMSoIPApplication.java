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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.R;
import de.christl.smsoip.annotations.APIVersion;
import de.christl.smsoip.autosuggest.NameNumberEntry;
import de.christl.smsoip.database.DatabaseHandler;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.IOException;
import java.util.*;

@ReportsCrashes(formKey = "dDFEZFF5R1FzTWlNVHkzdnJFaGVDTUE6MQ", mode = ReportingInteractionMode.NOTIFICATION,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
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
    private HashMap<String, SMSoIPPlugin> pluginsToOld = new HashMap<String, SMSoIPPlugin>();
    private HashMap<String, SMSoIPPlugin> pluginsToNew = new HashMap<String, SMSoIPPlugin>();
    private ArrayList<SMSoIPPlugin> plugins;
    private boolean writeToDatabaseAvailable = false;
    private boolean adsEnabled = true;
    private int versionNumber;
    private Integer installedPackages;
    private static Activity currentActivity;
    private List<NameNumberEntry> allContactsWithPhoneNumber;

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
    public void initProviders() {
        try {
            List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);
//refresh only if not yet done and if a new application is installed
            if (installedPackages == null || !installedPackages.equals(installedApplications.size())) {
                versionNumber = ExtendedSMSSupplier.class.getAnnotation(APIVersion.class).minVersion();
                ACRA.getErrorReporter().putCustomData("APIVersion", String.valueOf(versionNumber));
                plugins = new ArrayList<SMSoIPPlugin>();
                for (ApplicationInfo installedApplication : installedApplications) {
                    if (installedApplication.processName.startsWith(PLUGIN_CLASS_PREFIX)) {
                        Resources resourcesForApplication = getPackageManager().getResourcesForApplication(installedApplication);
                        PackageInfo packageInfo = getPackageManager().getPackageInfo(installedApplication.packageName, 0);
                        plugins.add(new SMSoIPPlugin(installedApplication, packageInfo, resourcesForApplication));
                    } else if (installedApplication.processName.startsWith(PLUGIN_ADFREE_PREFIX)) {
                        adsEnabled = false;
                    }
                }
                readOutPlugins();
                installedPackages = installedApplications.size();
            }

        } catch (IllegalAccessException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
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
            PathClassLoader pathClassLoader = new PathClassLoader(sourceDir, getClassLoader());
            plugin.setClassLoader(pathClassLoader);
            Enumeration<String> classFileEntries = apkDir.entries();
            while (classFileEntries.hasMoreElements()) {
                String s = classFileEntries.nextElement();
                if (!s.startsWith(PLUGIN_CLASS_PREFIX)) {
                    continue;
                }
                plugin.addAvailableClass(s);
                try {
                    Class<?> aClass = Class.forName(s, false, pathClassLoader);
                    Class<?>[] aClassInterfaces = aClass.getInterfaces();
                    if (aClassInterfaces != null && ExtendedSMSSupplier.class.isAssignableFrom(aClass)) {

                        int minVersion = getPluginsMinApiVersion((Class<ExtendedSMSSupplier>) aClass);
                        ExtendedSMSSupplier smsSupplier = (ExtendedSMSSupplier) aClass.newInstance();
                        plugin.setMinAPIVersion(minVersion);
                        plugin.setSupplier(smsSupplier);
                        if (versionNumber > minVersion) {
                            pluginsToOld.put(aClass.getCanonicalName(), plugin);
                            break;
                        } else if (minVersion > versionNumber) {
                            pluginsToNew.put(aClass.getCanonicalName(), plugin);
                            break;

                        }
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
            builder.append(smSoIPPlugin.getProviderName()).append(":").append(smSoIPPlugin.getMinAPIVersion());
            builder.append(" ").append(smSoIPPlugin.getVersion()).append("\n");
        }
        builder.append("<to old>:\n");
        for (Map.Entry<String, SMSoIPPlugin> stringSMSoIPPluginEntry : pluginsToOld.entrySet()) {
            SMSoIPPlugin smSoIPPlugin = stringSMSoIPPluginEntry.getValue();
            String canonicalName = stringSMSoIPPluginEntry.getKey();
            if (smSoIPPlugin.getSupplier() != null) {
                builder.append(smSoIPPlugin.getProviderName()).append(":").append(smSoIPPlugin.getMinAPIVersion());
                builder.append(" ").append(smSoIPPlugin.getVersion());
            } else {
                builder.append(canonicalName);
            }
            builder.append("\n");
        }
        builder.append("<to new>:\n");
        for (Map.Entry<String, SMSoIPPlugin> stringSMSoIPPluginEntry : pluginsToNew.entrySet()) {
            SMSoIPPlugin smSoIPPlugin = stringSMSoIPPluginEntry.getValue();
            String canonicalName = stringSMSoIPPluginEntry.getKey();
            if (smSoIPPlugin.getSupplier() != null) {
                builder.append(smSoIPPlugin.getProviderName()).append(":").append(smSoIPPlugin.getMinAPIVersion());
                builder.append(" ").append(smSoIPPlugin.getVersion());
            } else {
                builder.append(canonicalName);
            }
            builder.append("\n");
        }
        ACRA.getErrorReporter().putCustomData("PLUGINS", builder.toString());
    }

    /**
     * find out the correct API Version defined by interface used
     *
     * @param aClass Interface extending SMSSupplier
     * @return the found version
     */
    private int getPluginsMinApiVersion(Class<ExtendedSMSSupplier> aClass) {
        int out = 13;
        if (aClass.getSuperclass() == null || aClass.getSuperclass().equals(Object.class)) {
            Class<?>[] interfaces = aClass.getInterfaces();
            boolean found = false;
            for (Class<?> anInterface : interfaces) {
                APIVersion annotation = anInterface.getAnnotation(APIVersion.class);
                if (annotation != null) {
                    out = annotation.minVersion();
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (Class<?> anInterface : interfaces) {
                    if (ExtendedSMSSupplier.class.isAssignableFrom(anInterface)) {
                        return getPluginsMinApiVersion((Class<ExtendedSMSSupplier>) anInterface);
                    }
                }
            }
        } else if (aClass.equals(ExtendedSMSSupplier.class)) {
            return out;
        } else {
            return getPluginsMinApiVersion((Class<ExtendedSMSSupplier>) aClass.getSuperclass());
        }
        return out;
    }

    public static SMSoIPApplication getApp() {
        return app;
    }

    public Map<String, SMSoIPPlugin> getProviderEntries() {
        return loadedProviders;
    }

    public int getMinAPIVersion(ExtendedSMSSupplier smsSupplier) {
        return loadedProviders.get(smsSupplier.getClass().getCanonicalName()).getMinAPIVersion();
    }

//    @SuppressWarnings("unchecked")
//    public <TYPE> TYPE getInstance(String className) {
//        try {
//            ClassLoader pathClassLoader = getClassLoaderForClass(className);
//            if (pathClassLoader == null) {
//                return null;
//            }
//            Class clazz = Class.forName(className, false, pathClassLoader);
//            return (TYPE) clazz.newInstance();
//        } catch (IllegalAccessException e) {
//            Log.e(this.getClass().getCanonicalName(), "", e);
//        } catch (InstantiationException e) {
//            Log.e(this.getClass().getCanonicalName(), "", e);
//        } catch (ClassNotFoundException e) {
//            Log.e(this.getClass().getCanonicalName(), "", e);
//        }
//        return null;
//    }

    public SMSoIPPlugin getSMSoIPPluginBySupplierName(String className) {
        return loadedProviders.get(className);
    }

    private ClassLoader getClassLoaderForClass(String className) {
        for (SMSoIPPlugin plugin : plugins) {
            if (plugin.isClassAvailable(className)) {
                return plugin.getClassLoader();
            }
        }
        return null;
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

    public List<NameNumberEntry> getContactsWithPhoneNumberList() {
        if (allContactsWithPhoneNumber == null) {
            allContactsWithPhoneNumber = DatabaseHandler.getAllContactsWithPhoneNumber(this);
        }
        return allContactsWithPhoneNumber;
    }
}
