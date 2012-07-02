package de.christl.smsoip.application;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.R;
import de.christl.smsoip.annotations.APIVersion;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.IOException;
import java.util.*;

@ReportsCrashes(formKey = "dDQ4RzRTaGxfZHdLZlNtU2gtTWtDOVE6MQ", mode = ReportingInteractionMode.NOTIFICATION,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resNotifTickerText = R.string.crash_notif_ticker_text,
        resNotifTitle = R.string.crash_notif_title,
        resNotifText = R.string.crash_notif_text,
        resNotifIcon = android.R.drawable.stat_notify_error, // optional. default is a warning sign
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast)
public class SMSoIPApplication extends Application {

    private static SMSoIPApplication app;
    public static final String PLUGIN_CLASS_PREFIX = "de.christl.smsoip.supplier";
    public static final String PLUGIN_ADFREE_PREFIX = "de.christl.smsoip.adfree";
    private Map<String, ProviderEntry> loadedProviders = new HashMap<String, ProviderEntry>();
    private List<ExtendedSMSSupplier> pluginsToOld = new ArrayList<ExtendedSMSSupplier>();
    private List<ExtendedSMSSupplier> pluginsToNew = new ArrayList<ExtendedSMSSupplier>();
    private ArrayList<SMSoIPPlugin> plugins;
    private boolean writeToDatabaseAvailable = false;
    private boolean adsEnabled = true;
    private int versionNumber;
    private Integer installedPackages;

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
            String type = getContentResolver().getType(Uri.parse("content://sms/sent"));
            if (type != null) {
                writeToDatabaseAvailable = true;
            }
        } catch (IllegalArgumentException e) {
            writeToDatabaseAvailable = false;
        }
    }

    public void initProviders() {
        try {
            List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);
            //refresh only if not yet done and if a new application is installed
            if (installedPackages == null || !installedPackages.equals(installedApplications.size())) {
                PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionNumber = ((APIVersion) ExtendedSMSSupplier.class.getAnnotations()[0]).minVersion();
                plugins = new ArrayList<SMSoIPPlugin>();
                for (ApplicationInfo installedApplication : installedApplications) {
                    if (installedApplication.processName.startsWith(PLUGIN_CLASS_PREFIX)) {
                        Resources resourcesForApplication = getPackageManager().getResourcesForApplication(installedApplication);
                        plugins.add(new SMSoIPPlugin(installedApplication, resourcesForApplication));
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
        loadedProviders = new HashMap<String, ProviderEntry>();
        pluginsToNew = new ArrayList<ExtendedSMSSupplier>();
        pluginsToOld = new ArrayList<ExtendedSMSSupplier>();
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
                        if (versionNumber > minVersion) {
                            pluginsToOld.add(smsSupplier);
                            break;
                        } else if (minVersion > versionNumber) {
                            pluginsToNew.add(smsSupplier);
                            break;

                        }
                        loadedProviders.put(aClass.getCanonicalName(), new ProviderEntry(smsSupplier, minVersion));
                        break;
                    }
                } catch (ClassNotFoundException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                } catch (InstantiationException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                }
            }
        }
//        for (Map.Entry<String, ProviderEntry> loadedProvider : loadedProviders.entrySet()) {
        ErrorReporter.getInstance().putCustomData("SUCCESFUL", "myvalue");
//        }
//        for (ExtendedSMSSupplier extendedSMSSupplier : pluginsToOld) {
//            ErrorReporter.getInstance().putCustomData("toold" + extendedSMSSupplier.toString(), extendedSMSSupplier.toString());
//        }
//        for (ExtendedSMSSupplier extendedSMSSupplier : pluginsToNew) {
//            ErrorReporter.getInstance().putCustomData("toNew" + extendedSMSSupplier.toString(), extendedSMSSupplier.toString());
//        }
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

    public Map<String, ProviderEntry> getProviderEntries() {
        return loadedProviders;
    }

    public int getMinAPIVersion(ExtendedSMSSupplier smsSupplier) {
        return loadedProviders.get(smsSupplier.getClass().getCanonicalName()).getMinAPIVersion();
    }

    @SuppressWarnings("unchecked")
    public <TYPE> TYPE getInstance(String className) {
        try {
            ClassLoader pathClassLoader = getClassLoaderForClass(className);
            if (pathClassLoader == null) {
                return null;
            }
            Class clazz = Class.forName(className, false, pathClassLoader);
            return (TYPE) clazz.newInstance();
        } catch (IllegalAccessException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        } catch (InstantiationException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        } catch (ClassNotFoundException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }
        return null;
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

    public List<ExtendedSMSSupplier> getPluginsToOld() {
        return pluginsToOld;
    }

    public List<ExtendedSMSSupplier> getPluginsToNew() {
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
}
