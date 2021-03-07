package io.github.waikato_ufdl.ui.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A utility class to create and delete image files.
 */

public class ImageUtils {

    /***
     *  A method to create an image file in the pictures directory
     * @param context the context
     * @param image the image bitmap
     * @param name the name of the image
     * @return the filepath of the image if image creation is successful. Else, returns null.
     */
    public static String createImageFile(Context context, Bitmap image, String name) {
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) , name);
            FileOutputStream fOut = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /***
     * A method to delete a particular image file from the pictures directory
     * @param context the context
     * @param name the name of the image to delete
     */
    public static void deleteImageFile(Context context, String name) {
        try {
            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name + ".jpg");
            if (file.exists())
                file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
