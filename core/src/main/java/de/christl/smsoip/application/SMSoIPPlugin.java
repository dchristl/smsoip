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
import dalvik.system.PathClassLoader;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.provider.versioned.TimeShiftSupplier;
import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Simple class for managing plugins during read out
 */
public class SMSoIPPlugin {
    public static final String XMLID_TO_LOAD = "xmlid_to_load";
    private ApplicationInfo installedApplication;
    private PackageInfo packageInfo;
    private PathClassLoader pathClassLoader;
    private Set<String> availableClasses = new HashSet<String>();
    private ExtendedSMSSupplier supplier;
    private boolean timeShiftCapable = false;
    private Resources resources;

    public SMSoIPPlugin(ApplicationInfo installedApplication, PackageInfo packageInfo, PathClassLoader pathClassLoader) {
        this.installedApplication = installedApplication;
        this.packageInfo = packageInfo;
        this.pathClassLoader = pathClassLoader;
    }

    /**
     * the source directory for this plugin
     *
     * @return
     */
    public String getSourceDir() {
        return installedApplication.sourceDir;
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
                resources = SMSoIPApplication.getApp().getPackageManager().getResourcesForApplication(installedApplication);
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

    //removeIt

    /**
     * currently used for debug infos
     */
    public void putPluginInformation() {
        ErrorReporter errorReporter = ACRA.getErrorReporter();
        int classIt = 0;
        for (String availableClass : availableClasses) {
            errorReporter.putCustomData("availableClass" + classIt++, availableClass);
            final Field[] fields;
            if (availableClass.contains(".R$")) {
                try {
                    fields = Class.forName(availableClass, false, pathClassLoader).getDeclaredFields();
                    StringBuilder content = new StringBuilder();
                    for (Field field : fields) {
                        try {
                            Object o = field.get(new Object());
                            content.append(String.valueOf(field)).append("-->").append(String.valueOf(o));
                        } catch (Exception ignored) {
                        }
                    }
                    errorReporter.putCustomData(availableClass, content.toString());
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
//        errorReporter.handleSilentException(new IllegalArgumentException("wanted behaviour"));
        try {
            String customData = errorReporter.getCustomData(XMLID_TO_LOAD);
            int xmlid = Integer.parseInt(customData);
            InputStream inputStream = resolveRawResource(xmlid);
            int length = inputStream.available();
            errorReporter.putCustomData("xml_stream_length", String.valueOf(length));
            String inputStreamString = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
            System.out.println(inputStreamString.length());
        } catch (Exception e) {
            errorReporter.handleSilentException(e);
        }

    }
}
