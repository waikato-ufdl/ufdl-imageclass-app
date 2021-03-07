package io.github.waikato_ufdl;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.IOException;
import java.util.List;

import io.github.waikato_ufdl.ui.camera.ClassifierDetails;
import io.github.waikato_ufdl.ui.camera.PyTorchClassifier;
import io.github.waikato_ufdl.ui.camera.TFLiteClassifier;


public abstract class Classifier {
    protected final int TOP_K = 3;

    /***
     * Default constructor for a classifier object
     */
    protected Classifier() {
    }

    /***
     * A factory method to return an appropriate image classifier depending on the classifier
     * details provided, specifically, the model extension.
     * @param context the context
     * @param details the classifier details
     * @return a PyTorch classifier or Tensorflow lite classifier depending on the model extension.
     */
    public static Classifier createInstance(Context context, ClassifierDetails details) {
        try {
            if (details.getName().endsWith(".pt")) {
                return new PyTorchClassifier(context, details);
            } else {
                return new TFLiteClassifier(context, details);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /***
     * An abstract method to return a list of predictions
     * @param bitmap the bitmap to feed the image classifier
     * @return a list of the top K (k = 3) predictions
     */
    public abstract List<Prediction> topKPredictions(Bitmap bitmap);

    /***
     * An abstract method to return a single prediction
     * @param bitmap the bitmap to feed to the image classifier
     * @return a Prediction
     */
    public abstract Prediction predict(Bitmap bitmap);
}
