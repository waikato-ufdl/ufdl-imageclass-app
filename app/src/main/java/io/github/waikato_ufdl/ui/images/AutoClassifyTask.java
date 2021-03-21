package io.github.waikato_ufdl.ui.images;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;

import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.ImageOperations;
import io.github.waikato_ufdl.Prediction;

/**
 * A task used to automate the labelling process using a classifier to label images
 */

public abstract class AutoClassifyTask extends NetworkTask {
    private final ArrayList<ClassifiedImage> images;
    protected int index;
    private final Classifier classifier;
    private final double confidence;

    /***
     * The constructor for generating an auto-classification task
     * @param context the context
     * @param images the list of images to auto-classify
     * @param classifier the classifier
     * @param confidence the minimum confidence score required for the classify to overwrite the current label
     * @param datasetName the name of the dataset in which the images are being classified
     */
    public AutoClassifyTask(Context context, ArrayList<ClassifiedImage> images, Classifier classifier, double confidence, String datasetName) {
        super(context, datasetName);
        index = 0;
        processingMessage = "Classifying image ";
        completedMessage = "Successfully re-classified selected images";
        this.images = images;
        this.classifier = classifier;
        this.confidence = confidence;
    }

    /***
     * Method to reclassify images with labels predicted by a classifier in a background thread.
     * @param image the classified image object
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
     * Execute the auto-classification task.
     */
    @Override
    public void execute() {
        run(images);
    }

    /**
     * Define what should occur upon completion of the auto-completion task.
     */
    @Override
    public abstract void runOnCompletion();
}
