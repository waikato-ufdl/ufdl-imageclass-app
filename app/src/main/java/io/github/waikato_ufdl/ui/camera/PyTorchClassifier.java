package io.github.waikato_ufdl.ui.camera;


import android.content.Context;
import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.Prediction;


public class PyTorchClassifier extends Classifier {
    private final int width;
    private final int height;
    private final Module model;
    private final String[] classes;

    /***
     * Constructor to create a PyTorch Mobile Classifier
     * @param context the context
     * @param details the classifier details required to create the classifier
     */
    public PyTorchClassifier(Context context, ClassifierDetails details) {
        super();
        String modelPath = ClassifierUtils.assetFilePath(context, details.getName());
        this.model = Module.load(modelPath);
        this.width = details.getWidth();
        this.height = details.getHeight();
        this.classes = details.getClasses();
    }

    /***
     * Method to pre-process the bitmap and create a Tensor object
     * @param bitmap the image bitmap
     * @param width the image width expected by the model
     * @param height the image height expected by the model
     * @return Tensor object
     */
    public Tensor preprocess(Bitmap bitmap, int width, int height) {

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        return TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
    }

    /***
     * Method to find the index position of highest confidence score given an array of scores
     * @param scores the array of confidence scores
     * @return the index position of the highest confidence score
     */
    public int argMax(float[] scores) {
        int maxIndex = -1;
        float maxvalue = 0.0f;

        for (int i = 0; i < scores.length; i++) {

            if (scores[i] > maxvalue) {

                maxIndex = i;
                maxvalue = scores[i];
            }
        }

        return maxIndex;
    }


    /***
     * Method to feed a bitmap image to the classifier and return a prediction
     * @param bitmap the bitmap to feed to the image classifier
     * @return the Prediction produced by the classifier for the given bitmap
     */
    public Prediction predict(Bitmap bitmap) {
        Tensor tensor = preprocess(bitmap, width, height);
        IValue inputs = IValue.from(tensor);
        Tensor outputs = model.forward(inputs).toTensor();
        float[] scores = outputs.getDataAsFloatArray();

        int classIndex = argMax(scores);

        return new Prediction(classes[classIndex], scores[classIndex]);
    }


    /***
     * A method to feed a bitmap to the classifier and get the top 3 predictions produced by the classifier
     * @param bitmap the bitmap to feed the image classifier
     * @return A list containing the top 3 predictions produced by the classifier
     */
    public List<Prediction> topKPredictions(Bitmap bitmap) {
        List<Prediction> predictions = new ArrayList<>(TOP_K);
        Tensor tensor = preprocess(bitmap, width, height);
        IValue inputs = IValue.from(tensor);
        Tensor outputs = model.forward(inputs).toTensor();
        float[] scores = outputs.getDataAsFloatArray();
        int[] indices = topK(scores);

        for (int i = 0; i < TOP_K; i++) {
            int index = indices[i];

            String label = classes[index];
            Float confidence = scores[index];

            predictions.add(new Prediction(label, confidence));
        }

        return predictions;
    }

    /***
     * Method to get the indices of the top 3 prediction confidence scores
     * @param scores prediction scores
     * @return indices of the top 3 predictions
     */
    protected int[] topK(float[] scores) {
        float[] values = new float[TOP_K];
        Arrays.fill(values, -Float.MAX_VALUE);
        int[] indices = new int[TOP_K];
        Arrays.fill(indices, -1);

        for (int i = 0; i < scores.length; i++) {
            for (int j = 0; j < TOP_K; j++) {
                if (scores[i] > values[j]) {
                    for (int k = TOP_K - 1; k >= j + 1; k--) {
                        values[k] = values[k - 1];
                        indices[k] = indices[k - 1];
                    }
                    values[j] = scores[i];
                    indices[j] = i;
                    break;
                }
            }
        }
        return indices;
    }
}
