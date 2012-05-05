package de.christl.smsoip.application;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import dalvik.system.PathClassLoader;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple class for
 */
public class SMSoIPPlugin {
    private ApplicationInfo installedApplication;
    private Resources resources;
    private PathClassLoader classLoader;
    private Set<String> availableClasses = new HashSet<String>();

    public SMSoIPPlugin(ApplicationInfo installedApplication, Resources resourcesForApplication) {
        this.installedApplication = installedApplication;
        resources = resourcesForApplication;
    }

    public String getSourceDir() {
        return installedApplication.sourceDir;
    }

    public void setClassLoader(PathClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void addAvailableClass(String s) {
        availableClasses.add(s);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }


    public boolean isClassAvailable(String className) {
        for (String availableClass : availableClasses) {
            if (availableClass.equals(className)) {
                return true;
            }
        }
        return false;
    }

    public String resolveResource(int resourceId) {
        return resources.getString(resourceId);
    }
}
