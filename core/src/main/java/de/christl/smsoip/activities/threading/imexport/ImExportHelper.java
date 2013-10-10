/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.activities.threading.imexport;

import android.content.Context;
import android.os.Environment;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 */
public abstract class ImExportHelper {

    static final String SAMSUNG_SHARED_PREF_DIR = "/dbdata/databases/%s/shared_prefs/";
    public static final String ZIP_FILE_NAME = "backup";

    private ImExportHelper() {
    }


    static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    static File getExportDir() {
        String externalStorage = Environment.getExternalStorageDirectory().toString();
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        return new File(externalStorage + "/smsoip_backup");
    }

    static boolean cleanupAndCreate(File exportDir) {
        if (exportDir.exists()) {
            deleteFolder(exportDir);
        }
        return exportDir.mkdirs();
    }

    static File getDataDir(Context context) {
        String packageName = context.getPackageName();
        File samsungDataDir = new File(String.format(ImExportHelper.SAMSUNG_SHARED_PREF_DIR, packageName));
        File dataDir;
        if (samsungDataDir.exists()) {
            dataDir = samsungDataDir;
        } else {
            dataDir = new File(context.getFilesDir() + "/../shared_prefs");
        }
        return dataDir;
    }

    static boolean copyFileToDir(File exportDir, File file) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(file);
            File outFile = new File(exportDir, file.getName());
            out = new FileOutputStream(outFile);
            ImExportHelper.copyFile(in, out);
        } catch (FileNotFoundException ignored) {
            ACRA.getErrorReporter().handleSilentException(ignored);
            return false;
        } catch (IOException ignored) {
            ACRA.getErrorReporter().handleSilentException(ignored);
            return false;
        } finally {

            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (IOException ignored) {
                ACRA.getErrorReporter().handleSilentException(ignored);
            }
        }
        return true;
    }


    static boolean createZipFile(File exportDir, File[] files) {
        boolean success = true;
        byte[] buffer = new byte[1024];
        ZipOutputStream zos = null;
        try {
            FileOutputStream fos = new FileOutputStream(new File(exportDir, ZIP_FILE_NAME));
            zos = new ZipOutputStream(fos);
            for (File file : files) {
                String fileName = file.getName();
                if ((fileName.endsWith(".xml") && !fileName.contains("MCConfig")) || fileName.contains("background_")) {
                    ZipEntry ze = new ZipEntry(fileName);
                    zos.putNextEntry(ze);
                    FileInputStream in = new FileInputStream(file);
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }

                    in.close();
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            ACRA.getErrorReporter().handleSilentException(e);
            success = false;
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException ignored) {
                }
            }
        }
        return success;
    }

}
