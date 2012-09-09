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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
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

    public static boolean decodeAndSaveImages(String imagePath, int adjustment) {
        if (imagePath == null) {
            removeBackgroundImages();
            return true;
        }
        WindowManager wm = (WindowManager) SMSoIPApplication.getApp().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int widthPortrait = display.getWidth();
        int heightPortrait = display.getHeight() - adjustment;
        int widthLandscape = display.getHeight();
        int heightLandscape = display.getWidth() - adjustment;
        ByteArrayInputStream portrait;
        ByteArrayInputStream landscape;
        if (widthPortrait < heightPortrait) { //its a "normal" screen
            portrait = decodeImage(imagePath, widthPortrait, heightPortrait);
            landscape = decodeImage(imagePath, widthLandscape, heightLandscape);
        } else {
            landscape = decodeImage(imagePath, widthPortrait, heightPortrait);
            portrait = decodeImage(imagePath, widthLandscape, heightLandscape);
        }
        boolean out = false;
        if (portrait != null && landscape != null) {
            out = saveImage(portrait, BACKGROUND_IMAGE_PATH_PORTRAIT);
            out &= saveImage(landscape, BACKGROUND_IMAGE_PATH_LANDSCAPE);
        }
        return out;
    }

    private static ByteArrayInputStream decodeImage(String imagePath, int width, int height) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Uri parse = Uri.parse(imagePath);
        InputStream inputStream;
        try {
            inputStream = SMSoIPApplication.getApp().getContentResolver().openInputStream(parse);
        } catch (FileNotFoundException e) {
            return null;
        }
        BitmapFactory.decodeStream(inputStream, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, width, height);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        try {
            inputStream = SMSoIPApplication.getApp().getContentResolver().openInputStream(parse);
        } catch (FileNotFoundException e) {
            return null;
        }
        Bitmap bitmap = calculateRatio(width, height, BitmapFactory.decodeStream(inputStream, null, options), options);
        boolean success = false;
        if (bitmap != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            return new ByteArrayInputStream(bos.toByteArray());
        }
        return null;
    }

    private static Bitmap calculateRatio(int screenWidth, int screenHeight, Bitmap origin, BitmapFactory.Options options) {
        float screenRatio = (float) screenHeight / screenWidth;
        int newImageWidth = options.outWidth;
        int newImageHeight = options.outHeight;
        int horAdjustment = 0;
        int verAdjustment = 0;
        if (screenRatio < 1) {
            verAdjustment = -1;
            while (verAdjustment < 0) {
                newImageHeight = (int) (newImageWidth * screenRatio);
                verAdjustment = (options.outHeight - newImageHeight) / 2;
                if (verAdjustment < 0) {
                    if (newImageWidth - 50 > 0) {
                        newImageWidth -= 50;
                        horAdjustment = (options.outWidth - newImageWidth) / 2;
                    } else {
                        return null; //some strange bitmap
                    }
                }
            }
        } else {
            horAdjustment = -1;

            while (horAdjustment < 0) {
                screenRatio = (float) screenWidth / screenHeight;
                newImageWidth = (int) (newImageHeight * screenRatio);
                horAdjustment = (options.outWidth - newImageWidth) / 2;
                if (horAdjustment < 0) {
                    if (newImageHeight - 50 > 0) {
                        newImageHeight -= 50;
                        verAdjustment = (options.outHeight - newImageHeight) / 2;
                    } else {
                        return null; //some strange bitmap
                    }
                }
            }
        }
        Log.e("christl", "verAdjustment = " + verAdjustment);
        Log.e("christl", "horAdjustment = " + horAdjustment);
        Log.e("christl", "newImageHeight = " + newImageHeight);
        Log.e("christl", "newImageWidth = " + newImageWidth);
        Log.e("christl", "screenRatio = " + screenRatio);
        Log.e("christl", "screenWidth = " + screenWidth);
        Log.e("christl", "screenHeight = " + screenHeight);
        return Bitmap.createBitmap(origin, horAdjustment, verAdjustment, newImageWidth, newImageHeight);
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