package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;

import java.util.ArrayList;
import java.util.Collections;

import io.github.waikato_ufdl.ImageOperations;

/**
 * A task used to reclassify all selected images with a label predicted by a classifier
 */

public class AutoClassifyTask extends NetworkTask {
    private final ArrayList<ClassifiedImage> images;
    private final ArrayList<String> labels;
    protected int index;

    /***
     * The constructor for generating an AutoClassification task
     * @param fragment the fragment
     * @param context the context
     * @param images the arraylist of images to update
     * @param labels the new classifcation labels for the images
     * @param datasetName the name of the dataset in which the images belong
     * @param mode the action mode
     */
    public AutoClassifyTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, ArrayList<String> labels, String datasetName, ActionMode mode) {
        super(fragment, context, datasetName, mode);

        index = 0;
        processingMessage = "Uploading image ";
        completedMessage = "Successfully uploaded selected images";
        this.images = images;
        this.labels = labels;
    }

    /***
     * Method to reclassify images with labels predicted by a classifier
     * @param image either a ClassifiedImage object or URI object
     */
    @Override
    public void backgroundTask(Object image) {
        String imageFileName = ((ClassifiedImage) image).getImageFileName();
        String oldLabel = ((ClassifiedImage) image).getClassificationLabel();
        String label = labels.get(index);
        ImageOperations.updateImage(dbManager, datasetPK, datasetName, imageFileName, Collections.singletonList(oldLabel), label);
        index++;
    }

    /***
     * Execute the reclassification task
     */
    @Override
    public void execute() {
        run(images);
    }
}
