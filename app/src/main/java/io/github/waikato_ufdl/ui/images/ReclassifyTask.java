package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;

import java.util.ArrayList;
import java.util.Collections;

import io.github.waikato_ufdl.ImageOperations;

/**
 * A ReclassifyTask will be used to reclassify all selected images with a single given label
 */

public class ReclassifyTask extends NetworkTask {
    private final ArrayList<ClassifiedImage> selectedImages;
    private final String label;

    /**
     * The constructor for generating an UploadTask
     *
     * @param fragment    the ImagesFragment
     * @param context     the context
     * @param images      the list of images selected by the user (ClassifiedImage objects)
     * @param label       the classification label to allocate to all selected images
     * @param datasetName the name of the dataset in which the image belongs
     * @param mode        the action mode
     */
    public ReclassifyTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, String label, String datasetName, ActionMode mode) {
        super(fragment, context, datasetName, mode);

        this.selectedImages = images;
        this.label = label;
        processingMessage = "Reclassifying image ";
        completedMessage = "The selected images have been reclassified as: " + label;
    }

    /***
     * Method to reclassify images with the given classification label
     * @param image either a ClassifiedImage object or URI object
     */
    @Override
    public void backgroundTask(Object image) {
        String imageFileName = ((ClassifiedImage) image).getImageFileName();
        String classificationLabel = ((ClassifiedImage) image).getClassificationLabel();
        ImageOperations.updateImage(dbManager, datasetPK, datasetName, imageFileName, Collections.singletonList(classificationLabel), label);
    }

    /**
     * execute the reclassification task on the selected images
     */
    @Override
    public void execute() {
        run(selectedImages);
    }
}
