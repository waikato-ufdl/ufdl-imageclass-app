package io.github.waikato_ufdl.ui.camera;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static io.github.waikato_ufdl.ui.camera.cameraFragment.removeFileExtension;

/***
 * A utility class to get classifier details from the assets folder in order to build a classifier.
 */

public class ClassifierUtils {

    /***
     * A method to find the absolute file path of a model in the assets folder.
     * @param context the context
     * @param assetName the name of the model
     * @return the absolute file path of the model. Return null if the model with the given name doesn't exist in assets folder.
     */
    public static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("TAG", "Error: Cannot find the file path of " + assetName);
        }
        return null;
    }

    /***
     * A method to deserialize JSON model data to a ClassifierDetails object.
     * @param context the context
     * @param model the model name
     * @return Classifier details object. Returns null if model with the given name doesn't exist in asset folder.
     */
    public static ClassifierDetails deserializeModelJSON(Context context, String model) {
        try {
            String fileName = removeFileExtension(model, true);
            String filePath = assetFilePath(context, fileName + ".json");
            if (filePath != null) {
                return new ObjectMapper().
                        setSerializationInclusion(JsonInclude.Include.NON_NULL)
                        .readValue(new File(filePath), ClassifierDetails.class);
            }
        } catch (IOException e) {
            Log.e("TAG", e.getMessage());
        }

        return null;
    }
}
