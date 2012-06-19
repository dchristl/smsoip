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
import de.christl.smsoip.annotations.APIVersion;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class SMSoIPApplication extends Application {

    private static SMSoIPApplication app;
    public static final String PLUGIN_CLASS_PREFIX = "de.christl.smsoip.supplier";
    public static final String PLUGIN_ADFREE_PREFIX = "de.christl.smsoip.adfree";
    Map<String, ProviderEntry> loadedProviders = new HashMap<String, ProviderEntry>();
    List<SMSSupplier> pluginsToOld = new ArrayList<SMSSupplier>();
    List<SMSSupplier> pluginsToNew = new ArrayList<SMSSupplier>();
    private ArrayList<SMSoIPPlugin> plugins;
    private boolean writeToDatabaseAvailable = false;
    private boolean adsEnabled = true;
    private int versionNumber;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        setWriteToDBAvailable();
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
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionNumber = pinfo.versionCode;
            plugins = new ArrayList<SMSoIPPlugin>();
            for (ApplicationInfo installedApplication : installedApplications) {
                if (installedApplication.processName.startsWith(PLUGIN_CLASS_PREFIX)) {
                    Resources resourcesForApplication = getPackageManager().getResourcesForApplication(installedApplication);
                    plugins.add(new SMSoIPPlugin(installedApplication, resourcesForApplication));
                } else if (installedApplication.processName.startsWith(PLUGIN_ADFREE_PREFIX)) {
                    adsEnabled = false;
                }
            }
            for (SMSoIPPlugin plugin : plugins) {
                String sourceDir = plugin.getSourceDir();
                DexFile apkDir = new DexFile(sourceDir);
                PathClassLoader pathClassLoader = new PathClassLoader(sourceDir, getClassLoader());
                plugin.setClassLoader(pathClassLoader);
                Enumeration<String> classFileEntries = apkDir.entries();
                Outer:
                while (classFileEntries.hasMoreElements()) {
                    String s = classFileEntries.nextElement();
                    if (!s.startsWith(PLUGIN_CLASS_PREFIX)) {
                        continue;
                    }
                    plugin.addAvailableClass(s);
                    try {
                        Class<?> aClass = Class.forName(s, false, pathClassLoader);
                        Class<?>[] aClassInterfaces = aClass.getInterfaces();
                        if (aClassInterfaces != null) {
                            if (SMSSupplier.class.isAssignableFrom(aClass)) {

                                APIVersion annotation = aClass.getAnnotation(APIVersion.class);
                                int minVersion = annotation == null ? 13 : annotation.minVersion();
                                SMSSupplier smsSupplier = (SMSSupplier) aClass.newInstance();
                                if (versionNumber > minVersion) {
                                    List<Method> interfaceMethods = Arrays.asList(SMSSupplier.class.getDeclaredMethods());
                                    for (Method interfaceMethod : interfaceMethods) {  //check of all methods in interface are there and if signature fits
                                        try {
                                            aClass.getDeclaredMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
                                        } catch (NoSuchMethodException e) { //method does not exist, means old plugin
                                            pluginsToOld.add(smsSupplier);
                                            break Outer;
                                        }
                                    }
                                } else if (minVersion > versionNumber) {
                                    pluginsToNew.add(smsSupplier);
                                    break;

                                }
                                loadedProviders.put(aClass.getCanonicalName(), new ProviderEntry(smsSupplier, minVersion));
                                break;
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        Log.e(this.getClass().getCanonicalName(), "", e);
                    } catch (InstantiationException e) {
                        Log.e(this.getClass().getCanonicalName(), "", e);
                    }
                }
            }

        } catch (IllegalAccessException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
        }
    }

    public static SMSoIPApplication getApp() {
        return app;
    }

    public Map<String, ProviderEntry> getProviderEntries() {
        return loadedProviders;
    }

    @SuppressWarnings("unchecked")
    public <TYPE> TYPE getInstance(String className) {
        try {
            ClassLoader pathClassLoader = getClassLoaderForClass(className);
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
        return getClassLoader();
    }


    private SMSoIPPlugin getPluginForClass(String className) {
        for (SMSoIPPlugin plugin : plugins) {
            if (plugin.isClassAvailable(className)) {
                return plugin;
            }
        }
        return null;
    }

    public List<SMSSupplier> getPluginsToOld() {
        return pluginsToOld;
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
