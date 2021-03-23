package io.github.waikato_ufdl.ui.camera;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.waikato_ufdl.Classifier;
import io.github.waikato_ufdl.Prediction;

public class TFLiteClassifier extends Classifier {

    /***
     * Pre-processing normalization parameters
     */
    private static float IMAGE_MEAN;
    private static float IMAGE_STD;

    /***
     * Image size along the x axis.
     */
    private final int imageResizeX;

    /***
     * Image size along the y axis.
     */
    private final int imageResizeY;

    /***
     * Labels corresponding to the output of the vision model.
     */
    private final List<String> labels;

    /***
     * An instance of the driver class to run model inference with Tensorflow Lite.
     */
    private final Interpreter tensorClassifier;

    /***
     * Input image TensorBuffer.
     */
    private TensorImage inputImageBuffer;

    /***
     * Output probability TensorBuffer.
     */
    private final TensorBuffer probabilityImageBuffer;

    /***
     * Processor to apply post processing of the output probability.
     */
    private final TensorProcessor probabilityProcessor;


    /***
     * Creates a TensorFlow Lite classifier
     * @param context the context
     * @param details the required details to create classifier including model name, width, height and classes
     * @throws IOException if model fails to load from assets
     */
    public TFLiteClassifier(Context context, ClassifierDetails details) throws IOException {
        super();
        MappedByteBuffer classifierModel = FileUtil.loadModelFile(context, details.getName());
        labels = Arrays.asList(details.getClasses());
        tensorClassifier = new Interpreter(classifierModel, null);

        float[] preProcessingNormalizationParams = details.getPreProcessingNormalizationParams();
        float[] postProcessingNormalizationParams = details.getPostProcessingNormalizationParams();

        // set the normalization parameters */
        IMAGE_MEAN = preProcessingNormalizationParams[0];
        IMAGE_STD = preProcessingNormalizationParams[1];


         //Post-processing normalization parameters.
        float PROBABILITY_MEAN = postProcessingNormalizationParams[0];
        float PROBABILITY_STD = postProcessingNormalizationParams[1];

        // Reads type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0; // input
        int probabilityTensorIndex = 0;// output

        int[] inputImageShape = tensorClassifier.getInputTensor(imageTensorIndex).shape();
        DataType inputDataType = tensorClassifier.getInputTensor(imageTensorIndex).dataType();

        int[] outputImageShape = tensorClassifier.getOutputTensor(probabilityTensorIndex).shape();
        DataType outputDataType = tensorClassifier.getOutputTensor(probabilityTensorIndex).dataType();

        imageResizeY = inputImageShape[1];
        imageResizeX = inputImageShape[2];

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(inputDataType);

        // Creates the output tensor and its processor.
        probabilityImageBuffer = TensorBuffer.createFixedSize(outputImageShape, outputDataType);

        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD))
                .build();
    }

    /***
     * method runs the inference and returns a list of prediction results
     * @param bitmap the bitmap of the image
     * @return classification results
     */
    public List<Prediction> topKPredictions(final Bitmap bitmap) {
        return topKPredictions(bitmap, TOP_K);
    }

    /***
     * Method runs the inference and returns a single prediction
     * @param bitmap the bitmap of the image
     * @return a single Prediction
     */
    @Override
    public Prediction predict(Bitmap bitmap) {
        return topKPredictions(bitmap, 1).get(0);
    }

    /***
     * method runs the inference and returns the classification result
     * @param bitmap the bitmap of the image
     * @param k the number of prediction results to return
     * @return classification results
     */
    public List<Prediction> topKPredictions(final Bitmap bitmap, int k) {
        List<Prediction> recognitions = new ArrayList<>();

        inputImageBuffer = loadImage(bitmap, 0);
        tensorClassifier.run(inputImageBuffer.getBuffer(), probabilityImageBuffer.getBuffer().rewind());

        // Gets the map of label and probability.
        Map<String, Float> labelledProbability = new TensorLabel(labels,
                probabilityProcessor.process(probabilityImageBuffer)).getMapWithFloatValue();

        for (Map.Entry<String, Float> entry : labelledProbability.entrySet()) {
            recognitions.add(new Prediction(entry.getKey(), entry.getValue()));
        }

        // Find the best classifications by sorting predicitons based on confidence
        Collections.sort(recognitions);
        // returning top N predicitons
        return recognitions.subList(0, k);
    }


    /***
     * loads the image into tensor input buffer and apply pre processing steps
     * @param bitmap the bitmap to be loaded
     * @param sensorOrientation the sensor orientation
     * @return the image loaded tensor input buffer
     */
    private TensorImage loadImage(Bitmap bitmap, int sensorOrientation) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        int noOfRotations = sensorOrientation / 90;
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());

        // Creates processor for the TensorImage.
        // pre processing steps are applied here
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                .add(new ResizeOp(imageResizeY, imageResizeX, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                .add(new Rot90Op(noOfRotations))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();
        return imageProcessor.process(inputImageBuffer);
    }

    /***
     * FileUtil class to load data from asset files.
     */
    public static class FileUtil {

        private FileUtil() {
        }

        /***
         * Load TF Lite model from asset file.
         * @param context the context
         * @param modelPath the path of the model to load
         * @return MappedByteBuffer of the model
         * @throws IOException failure to load
         */
        public static MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {

            try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
                 FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
                FileChannel fileChannel = inputStream.getChannel();
                long startOffset = fileDescriptor.getStartOffset();
                long declaredLength = fileDescriptor.getDeclaredLength();
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            }
        }
    }
}
