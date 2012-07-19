package de.christl.smsoip.application;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple class for managing plugins during read out
 */
public class SMSoIPPlugin {
    private ApplicationInfo installedApplication;
    private PackageInfo packageInfo;
    private Resources resources;
    private PathClassLoader classLoader;
    private Set<String> availableClasses = new HashSet<String>();
    private int minAPIVersion;
    private ExtendedSMSSupplier supplier;
    private boolean timeShiftCapable = false;

    public SMSoIPPlugin(ApplicationInfo installedApplication, PackageInfo packageInfo, Resources resourcesForApplication) {
        this.installedApplication = installedApplication;
        this.packageInfo = packageInfo;
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

    public String resolveStringResource(int resourceId) {
        return resources.getString(resourceId);
    }

    public String resolveStringResource(int resourceId, int quantity) {
        return resources.getQuantityString(resourceId, quantity);
    }

    public String[] resolveArrayStringResource(int resourceId) {
        return resources.getStringArray(resourceId);
    }

    public Drawable resolveDrawable(int drawableId) {
        return resources.getDrawable(drawableId);
    }

    public String getSupplierClassName() {
        return supplier.getClass().getCanonicalName();
    }

    public void setMinAPIVersion(int minVersion) {
        this.minAPIVersion = minVersion;
    }

    public int getMinAPIVersion() {
        return minAPIVersion;
    }

    public void setSupplier(ExtendedSMSSupplier supplier) {
        this.supplier = supplier;
        if (TimeShiftSupplier.class.isAssignableFrom(supplier.getClass())) {
            timeShiftCapable = true;
        }
    }

    public String getProviderName() {
        return supplier.getProvider().getProviderName();
    }


    public String getVersion() {
        return packageInfo.versionName;
    }

    public boolean isTimeShiftCapable() {
        return timeShiftCapable;
    }

    public OptionProvider getProvider() {
        return supplier.getProvider();
    }

    public ExtendedSMSSupplier getSupplier() {
        return supplier;
    }

    public TimeShiftSupplier getTimeShiftSupplier() {
        return (TimeShiftSupplier) supplier;
    }
}
