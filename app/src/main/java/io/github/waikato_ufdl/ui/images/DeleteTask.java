package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;

import java.util.ArrayList;

import io.github.waikato_ufdl.ImageOperations;

/**
 * A DeleteTask will be used to delete selected images from the backend (if online mode) or
 * set the sync status of the images to delete (in offline mode).
 */

public class DeleteTask extends NetworkTask {
    private final ArrayList<ClassifiedImage> selectedImages;

    /**
     * The constructor for generating an UploadTask
     *
     * @param fragment the ImagesFragment
     * @param context  the context
     * @param images   the list of selected images (ClassifiedImage objects)
     * @param mode     the action mode
     */
    public DeleteTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, String datasetName, ActionMode mode) {
        super(fragment, context, datasetName, mode);

        this.selectedImages = images;
        processingMessage = "Deleting image ";
        completedMessage = "Successfully deleted selected images";
    }

    /**
     * Method to delete images from a dataset
     *
     * @param image the classified image object
     */
    @Override
    public void backgroundTask(Object image) {
        String imageFilename = ((ClassifiedImage) image).getImageFileName();
        String cachePath = ((ClassifiedImage) image).getCachedFilePath();
        ImageOperations.deleteImage(dbManager, datasetPK, datasetName, imageFilename, cachePath);
    }

    /**
     * Method will execute the deletion process and display a progress dialog to the user
     */
    @Override
    public void execute() {
        run(selectedImages);
    }
}
