package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;

import java.util.ArrayList;
import java.util.Collections;

import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.ImageOperations;
import io.github.waikato_ufdl.Prediction;

/**
 * A task used to reclassify all selected images with a label predicted by a classifier
 */

public class AutoClassifyTask extends NetworkTask {
    private final ArrayList<ClassifiedImage> images;
    protected int index;
    private final Classifier classifier;
    private final double confidence;

    public AutoClassifyTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, Classifier classifier, double confidence, String datasetName, ActionMode mode) {
        super(fragment, context, datasetName, mode);
        index = 0;
        processingMessage = "Classifying image ";
        completedMessage = "Successfully uploaded selected images";
        this.images = images;
        this.classifier = classifier;
        this.confidence = confidence;
    }

    /***
     * Method to reclassify images with labels predicted by a classifier
     * @param image either a ClassifiedImage object or URI object
     */
    @Override
    public void backgroundTask(Object image) {
        String imageFileName = ((ClassifiedImage) image).getImageFileName();
        String oldLabel = ((ClassifiedImage) image).getClassificationLabel();

        if (image != null) {
            Prediction prediction = classifier.predict(((ClassifiedImage) image).getImageBitmap());

            if (prediction.getConfidence() > confidence) {
                String label = prediction.getLabel();
                ImageOperations.updateImage(dbManager, datasetPK, datasetName, imageFileName, Collections.singletonList(oldLabel), label);
            }
        }
    }

    /***
     * Execute the reclassification task
     */
    @Override
    public void execute() {
        run(images);
    }
}
