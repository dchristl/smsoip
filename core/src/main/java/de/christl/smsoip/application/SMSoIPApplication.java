package de.christl.smsoip.application;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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

@ReportsCrashes(formKey = "dG1sVXFvbGprY25rbWQ2WEZ6SzlLaEE6MQ", mode = ReportingInteractionMode.NOTIFICATION,
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

    /**
     * read out all packages to find installed plugins
     */
    public void initProviders() {
        try {
            List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);
//refresh only if not yet done and if a new application is installed
            if (installedPackages == null || !installedPackages.equals(installedApplications.size())) {
                versionNumber = ExtendedSMSSupplier.class.getAnnotation(APIVersion.class).minVersion();
                ErrorReporter.getInstance().putCustomData("APIVersion", String.valueOf(versionNumber));
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
                } catch (InstantiationException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
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
        for (SMSoIPPlugin smSoIPPlugin : pluginsToOld.values()) {
            builder.append(smSoIPPlugin.getProviderName()).append(":").append(smSoIPPlugin.getMinAPIVersion());
            builder.append(" ").append(smSoIPPlugin.getVersion()).append("\n");
        }
        builder.append("<to new>:\n");
        for (SMSoIPPlugin smSoIPPlugin : pluginsToNew.values()) {
            builder.append(smSoIPPlugin.getProviderName()).append(":").append(smSoIPPlugin.getMinAPIVersion());
            builder.append(" ").append(smSoIPPlugin.getVersion()).append("\n");
        }
        ErrorReporter.getInstance().putCustomData("PLUGINS", builder.toString());
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
}
