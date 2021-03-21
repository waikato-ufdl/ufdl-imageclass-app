package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.List;

import io.github.waikato_ufdl.ImageOperations;
import io.github.waikato_ufdl.UriUtils;

/**
 * An UploadTask will be used to upload images selected from the user's gallery to the backend
 */
public abstract class UploadTask extends NetworkTask {
    private final List<Uri> galleryImages;
    private final String[] labels;
    protected int index;

    /**
     * The constructor for generating an UploadTask
     *
     * @param context       the context
     * @param galleryImages the list of URI's of images selected from gallery
     * @param labels        the classification labels associated with the selected images
     * @param datasetName   the name of the dataset being modified
     */
    public UploadTask(Context context, List<Uri> galleryImages, String[] labels, String datasetName) {
        super(context, datasetName);
        index = 0;
        processingMessage = "Uploading image ";
        completedMessage = "Successfully uploaded selected images";
        this.galleryImages = galleryImages;
        this.labels = labels;
    }

    /***
     * @param image The image URI
     */
    @Override
    public void backgroundTask(Object image) {
        String label = labels[index];
        String classificationLabel = (label != null && !label.isEmpty()) ? label : "-";
        String imagePath = UriUtils.getPath(context, (Uri) image);
        if(imagePath != null) {
            File imageFile = new File(imagePath);
            ImageOperations.uploadImage(dbManager, datasetPK, datasetName, imageFile, classificationLabel);
        }
        index++;
    }

    /***
     * will display a progress dialog whilst adding each image (along with its category) to the backend
     */
    @Override
    public void execute() {
        run(galleryImages);
    }


    /***
     * Method will set the retrievedAll variable to false so that the newly added images will be loaded from the backend
     */
    @Override
    public abstract void runOnCompletion();
}