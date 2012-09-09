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

package de.christl.smsoip.util;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Display;
import android.view.WindowManager;
import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;

import java.io.*;

/**
 * Helper method for processing bitmaps
 */
public class BitmapProcessor {

    private static final String BACKGROUND_IMAGE_PATH_PORTRAIT = "background_portrait";
    private static final String BACKGROUND_IMAGE_PATH_LANDSCAPE = "background_landscape";

    private static final int MAX_WIDTH = 480;
    private static final int MAX_HEIGHT = 800;

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int scaledSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                scaledSize = Math.round((float) height / (float) reqHeight);
            } else {
                scaledSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return scaledSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {

        // First decodeAndSaveImage with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static boolean decodeAndSaveImage(String imagePath) {
        if (imagePath == null) {
            removeBackgroundImages();
            return true;
        }
        WindowManager wm = (WindowManager) SMSoIPApplication.getApp().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Uri parse = Uri.parse(imagePath);
        InputStream inputStream;
        try {
            inputStream = SMSoIPApplication.getApp().getContentResolver().openInputStream(parse);
        } catch (FileNotFoundException e) {
            return false;
        }
        BitmapFactory.decodeStream(inputStream, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try {
            inputStream = SMSoIPApplication.getApp().getContentResolver().openInputStream(parse);
        } catch (FileNotFoundException e) {
            return false;
        }
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
        boolean success = false;
        if (bitmap != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            ByteArrayInputStream out = new ByteArrayInputStream(bos.toByteArray());
            success = saveImage(out, BACKGROUND_IMAGE_PATH_PORTRAIT);
            //rotate the image by 90 degrees
            Matrix matrix = new Matrix();
            matrix.postRotate(90f);
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            ByteArrayOutputStream landscapeBos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 0, landscapeBos);
            ByteArrayInputStream landscapeBis = new ByteArrayInputStream(bos.toByteArray());
            success &= saveImage(landscapeBis, BACKGROUND_IMAGE_PATH_LANDSCAPE);
        }
        return success;
    }

    private static void removeBackgroundImages() {
        SMSoIPApplication.getApp().deleteFile(BACKGROUND_IMAGE_PATH_PORTRAIT);
        SMSoIPApplication.getApp().deleteFile(BACKGROUND_IMAGE_PATH_LANDSCAPE);
    }


    public static Drawable getBackgroundImage(int orientation) {
        SMSoIPApplication app = SMSoIPApplication.getApp();
        Drawable out = app.getResources().getDrawable(R.drawable.background_holo_dark);
        String imageOrientation = BACKGROUND_IMAGE_PATH_PORTRAIT;
        try {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                imageOrientation = BACKGROUND_IMAGE_PATH_LANDSCAPE;
            }
            FileInputStream fileInputStream = app.openFileInput(imageOrientation);
            out = Drawable.createFromStream(fileInputStream, "");
        } catch (FileNotFoundException ignored) {
        }
        return out;
    }

    public static boolean isBackgroundImageSet() {
        try {
            SMSoIPApplication.getApp().openFileInput(BACKGROUND_IMAGE_PATH_PORTRAIT);
        } catch (FileNotFoundException e) {
            return false;
        }
        return true;
    }


    private static boolean saveImage(InputStream inputStream, String orientation) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(SMSoIPApplication.getApp().getFilesDir().getPath() + File.separator + orientation);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ignored) {
            }
        }
        return true;
    }
}