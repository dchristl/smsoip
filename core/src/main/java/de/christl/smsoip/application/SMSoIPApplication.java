package de.christl.smsoip.application;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.option.OptionProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SMSoIPApplication extends Application {

    private static SMSoIPApplication app;
    public static final String PLUGIN_CLASSNAME = "de.christl.smsoip.supplier";
    List<ProviderEntry> providers = new ArrayList<ProviderEntry>();
    private ArrayList<SMSoIPPlugin> plugins;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        initProviders();
    }

    private void initProviders() {
        try {
            List<ApplicationInfo> installedApplications = getPackageManager().getInstalledApplications(0);
            plugins = new ArrayList<SMSoIPPlugin>();
            for (ApplicationInfo installedApplication : installedApplications) {
                if (installedApplication.processName.startsWith(PLUGIN_CLASSNAME)) {
                    plugins.add(new SMSoIPPlugin(installedApplication));
                }
            }
            for (SMSoIPPlugin plugin : plugins) {
                String sourceDir = plugin.getSourceDir();
                DexFile apkDir = new DexFile(sourceDir);
                PathClassLoader pathClassLoader = new PathClassLoader(sourceDir, getClassLoader());
                plugin.setClassLoader(pathClassLoader);
                Enumeration<String> classFileEntries = apkDir.entries();
                while (classFileEntries.hasMoreElements()) {
                    String s = classFileEntries.nextElement();
                    plugin.addAvailableClass(s);
                    try {
                        Class<?> aClass = Class.forName(s, false, pathClassLoader);
                        Class<?> superclass = aClass.getSuperclass();
                        if (superclass != null && superclass.equals(OptionProvider.class)) {
                            OptionProvider optionProvider = (OptionProvider) aClass.newInstance();
                            providers.add(new ProviderEntry(optionProvider));
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
        }
    }

    public static SMSoIPApplication getApp() {
        return app;
    }

    public List<ProviderEntry> getProviderEntries() {
        return providers;
    }

    @SuppressWarnings("unchecked")
    public <TYPE> TYPE getInstance(String className) {
        try {
            ClassLoader pathClassLoader = getClassLoaderForClass(className);
            Class supplierClass = Class.forName(className, false, pathClassLoader);
            return (TYPE) supplierClass.newInstance();
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
}
