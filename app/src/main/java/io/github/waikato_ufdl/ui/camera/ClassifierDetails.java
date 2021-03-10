package io.github.waikato_ufdl.ui.camera;

/***
 A class to encapsulate the information required to build a Classifier
 */

public class ClassifierDetails {
    private String name;
    private int width;
    private int height;
    private String[] classes;

    //Required Normalization Parameters for TensorFlow Lite
    private float[] preProcessingNormalizationParams;   //[IMAGE_MEAN, IMAGE_STD]
    private float[] postProcessingNormalizationParams;  //[PROBABILITY_MEAN, PROBABILITY_STD]

    /***
     * Default constructor for ClassifierDetails
     */
    public ClassifierDetails() {
        super();
    }

    /***
     * getter method to return pre-processing normalization parameters used for TensorFlow Lite models
     * @return pre-processing normalization parameters
     */
    public float[] getPreProcessingNormalizationParams() {
        return preProcessingNormalizationParams;
    }

    /***
     * getter method to return post-processing normalization parameters used for TensorFlow Lite models
     * @return post-processing normalization parameters
     */
    public float[] getPostProcessingNormalizationParams() {
        return postProcessingNormalizationParams;
    }

    /***
     * Method to get the model name
     * @return the model name
     */
    public String getName() {
        return name;
    }

    /***
     * Method to get the recommended width of the input image for the particular classifier
     * @return recommended width of the input image
     */
    public int getWidth() {
        return width;
    }

    /***
     * Method to get the recommended height of the input image for the particular classifier
     * @return recommended height of the input image
     */
    public int getHeight() {
        return height;
    }

    /***
     * Method to get the set of classes the model was trained on
     * @return String array of classes (image labels)
     */
    public String[] getClasses() {
        return classes;
    }
}
