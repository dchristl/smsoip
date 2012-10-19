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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import dalvik.system.PathClassLoader;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

import java.io.InputStream;
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
        String supplierName = "";
        if (supplier == null) { //Plugin could not be loaded, so get the name from package
            for (String availableClass : availableClasses) {
                if (availableClass.contains(SMSoIPApplication.PLUGIN_CLASS_PREFIX)) {
                    supplierName = availableClass.replace(SMSoIPApplication.PLUGIN_CLASS_PREFIX + ".", "");
                    supplierName = supplierName.replaceAll("\\..*", "");
                    break;
                }
            }
        } else {
            supplierName = supplier.getProvider().getProviderName();
        }
        return supplierName;
    }


    public String getVersion() {
        return packageInfo.versionName;
    }

    public boolean isTimeShiftCapable(String sendType) {
        return timeShiftCapable && getTimeShiftSupplier().isSendTypeTimeShiftCapable(sendType);
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

    public int getVersionCode() {
        return packageInfo.versionCode;
    }

    public InputStream resolveRawResource(int resourceId) {
        return resources.openRawResource(resourceId);
    }
}
