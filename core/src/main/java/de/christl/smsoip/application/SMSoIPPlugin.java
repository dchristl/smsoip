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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.PathClassLoader;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;

/**
 * Simple class for managing plugins during read out
 */
public class SMSoIPPlugin {
    public static final String XMLID_TO_LOAD = "xmlid_to_load";
    private PackageInfo packageInfo;
    private PathClassLoader pathClassLoader;
    private Set<String> availableClasses = new HashSet<String>();
    private ExtendedSMSSupplier supplier;
    private boolean timeShiftCapable = false;
    private Resources resources;
    private String packageName;
    private String sourceDir;

    public SMSoIPPlugin(ApplicationInfo installedApplication, PackageInfo packageInfo, PathClassLoader pathClassLoader) {
        this.packageName = installedApplication.packageName;
        sourceDir = installedApplication.sourceDir;
        this.packageInfo = packageInfo;
        this.pathClassLoader = pathClassLoader;
    }

    /**
     * the source directory for this plugin
     *
     * @return
     */
    public String getSourceDir() {
        return sourceDir;
    }

    /**
     * add a class to available ones in this plugin
     *
     * @param s
     */
    public void addAvailableClass(String s) {
        availableClasses.add(s);
    }

    /**
     * check if a given class is in this package
     *
     * @param className
     * @return
     */
    public boolean isClassAvailable(String className) {
        for (String availableClass : availableClasses) {
            if (availableClass.equals(className)) {
                return true;
            }
        }
        return false;
    }

    /**
     * resolve a String from this package
     *
     * @param resourceId
     * @return
     */
    public synchronized String resolveStringResource(int resourceId) {
        return getResources().getString(resourceId);
    }

    //removeIt  or better refactor it
    private synchronized Resources getResources() {
        if (resources == null) {
            try {
                resources = SMSoIPApplication.getApp().getPackageManager().getResourcesForApplication(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                ACRA.getErrorReporter().handleSilentException(e);
            }
        }
        return resources;
    }

    /**
     * resolve a quantity string from this plugin
     *
     * @param resourceId
     * @param quantity
     * @return
     */
    public synchronized String resolveStringResource(int resourceId, int quantity) {
        return getResources().getQuantityString(resourceId, quantity);
    }

    /**
     * resolve an array string from this plugin
     *
     * @param resourceId
     * @return
     */
    public synchronized String[] resolveArrayStringResource(int resourceId) {
        return getResources().getStringArray(resourceId);
    }

    /**
     * resolve a drawable from this resource
     *
     * @param drawableId
     * @return
     */
    public synchronized Drawable resolveDrawable(int drawableId) {
        return getResources().getDrawable(drawableId);
    }

    /**
     * get full qualified supplier name for this plugin
     *
     * @return
     */
    public String getSupplierClassName() {
        return supplier.getClass().getCanonicalName();
    }

    /**
     * set the supplier and resolve if it is time shift capable
     *
     * @param supplier
     */
    public void setSupplier(ExtendedSMSSupplier supplier) {
        this.supplier = supplier;
        if (TimeShiftSupplier.class.isAssignableFrom(supplier.getClass())) {
            timeShiftCapable = true;
        }
    }

    /**
     * resolve the name of this provider
     *
     * @return
     */
    public String getProviderName() {
        String supplierName = "UNKNOWN";
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

    /**
     * get the current package version
     *
     * @return
     */
    public String getVersion() {
        return packageInfo.versionName;
    }

    /**
     * is time shift available
     *
     * @param sendType
     * @return
     */
    public boolean isTimeShiftCapable(String sendType) {
        return timeShiftCapable && getTimeShiftSupplier().isSendTypeTimeShiftCapable(sendType);
    }

    /**
     * get the corresponding provider for this plugin
     *
     * @return
     */
    public OptionProvider getProvider() {
        return supplier.getProvider();
    }

    /**
     * get the instantiated supplier
     *
     * @return
     */
    public ExtendedSMSSupplier getSupplier() {
        return supplier;
    }

    /**
     * get the getTimeShiftSupplier
     *
     * @return
     */
    public TimeShiftSupplier getTimeShiftSupplier() {
        return (TimeShiftSupplier) supplier;
    }

    /**
     * get the version code of this package
     *
     * @return
     */
    public int getVersionCode() {
        return packageInfo.versionCode;
    }

    /**
     * resolve a raw resurce from this plugin
     *
     * @param resourceId
     * @return
     */
    public synchronized InputStream resolveRawResource(int resourceId) {
        return getResources().openRawResource(resourceId);
    }

    /**
     * resolve a XML from this plugin
     *
     * @param xmlId
     * @return
     */
    public synchronized XmlResourceParser resolveXML(int xmlId) {
        ErrorReporter errorReporter = ACRA.getErrorReporter();
        errorReporter.putCustomData("xmlname_to_load", getResources().getResourceEntryName(xmlId));
        errorReporter.putCustomData(XMLID_TO_LOAD, String.valueOf(xmlId));
        XmlResourceParser xml = getResources().getXml(xmlId);
        errorReporter.putCustomData("xml_to_load", String.valueOf(xml));
        return xml;
    }

    /**
     * get the corresponding classloader
     *
     * @return
     */
    public PathClassLoader getPathClassLoader() {
        return pathClassLoader;
    }

    /**
     * get all classes from this plugin
     *
     * @return
     */
    public Set<String> getAvailableClasses() {
        return availableClasses;
    }


    public String getPackageName() {
        return packageName;
    }
}
