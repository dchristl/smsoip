package de.christl.smsoip.application;

import android.content.pm.ApplicationInfo;
import dalvik.system.PathClassLoader;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Danny
 * Date: 22.04.12
 * Time: 14:29
 * To change this template use File | Settings | File Templates.
 */
public class SMSoIPPlugin {
    private ApplicationInfo installedApplication;
    private PathClassLoader classLoader;
    private Set<String> availableClasses = new HashSet<String>();

    public SMSoIPPlugin(ApplicationInfo installedApplication) {
        this.installedApplication = installedApplication;
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
}
