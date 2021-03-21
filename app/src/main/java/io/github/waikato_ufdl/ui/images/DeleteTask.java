package io.github.waikato_ufdl.ui.images;

import android.content.Context;

import java.util.ArrayList;

import io.github.waikato_ufdl.ImageOperations;


/**
 * A DeleteTask will be used to delete selected images from the backend (if online mode) or
 * set the sync status of the images to delete (in offline mode).
 */

public abstract class DeleteTask extends NetworkTask {
    private final ArrayList<ClassifiedImage> selectedImages;

    /**
     * The constructor for generating an UploadTask
     *
     * @param context  the context
     * @param images   the list of selected images (ClassifiedImage objects)
     */
    public DeleteTask(Context context, ArrayList<ClassifiedImage> images, String datasetName) {
        super(context, datasetName);

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

    @Override
    public abstract void runOnCompletion();
}
