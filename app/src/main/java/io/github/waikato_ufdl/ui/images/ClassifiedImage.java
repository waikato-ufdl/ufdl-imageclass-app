package io.github.waikato_ufdl.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ClassifiedImage {
    private byte[] image;
    private String classification;
    private boolean selected;
    private final String filename;
    private String filepath;
    private String cachePath;
    private String datasetName;
    private int syncStatus;


    /***
     * Constructor for a classified Image object
     * @param image The byte array of the image
     * @param label The image's classification
     */
    public ClassifiedImage(byte[] image, String label, String filename) {
        this.image = image;
        this.classification = label;
        this.filename = filename;
        selected = false;
    }

    /***
     * Alternative constructor for a classified Image object
     * @param filename  the name of the image file
     * @param label     the classification label
     * @param cachePath the filepath of the cached image
     */
    public ClassifiedImage(String filename, String label, String cachePath) {
        this.filename = filename;
        this.classification = label;
        this.cachePath = cachePath;
        selected = false;
    }

    /***
     * Alternative constructor for a classified image object
     * @param filename the name of the image file
     * @param label the classification label
     * @param filepath the filepath of the full image
     * @param cachePath the filepath of the cached image
     */
    public ClassifiedImage(String filename, String label, String filepath, String cachePath) {
        this(filename, label, cachePath);
        this.filepath = filepath;
    }

    /***
     * Another constructor for a classified image object
     * @param datasetName the name of the dataset to store the image
     * @param filename the name of the image file
     * @param label the classification label
     * @param filepath the filepath of the full image
     * @param syncStatus the sync status of the image
     */
    public ClassifiedImage(String datasetName, String filename, String label, String filepath, int syncStatus) {
        this.datasetName = datasetName;
        this.filename = filename;
        this.classification = label;
        this.filepath = filepath;
        this.syncStatus = syncStatus;
    }

    /***
     * getter method to get the dataset name in which the image belongs
     * @return the name of the dataset in which the image belongs
     */
    public String getDatasetName() {
        return datasetName;
    }


    /***
     * getter method to get the sync status of the image
     * @return 0 = Synced, 1 = create, 2 = update and 3 = delete
     */
    public int getSyncStatus() {
        return syncStatus;
    }

    /***
     * Method to retrieve the classification label of the image
     * @return classification label of the image
     */
    public String getClassificationLabel() {
        return classification;
    }

    /***
     * Method to retrieve the image as a bitmap
     * @return image Bitmap
     */
    public Bitmap getImageBitmap() {
        String filePath;

        if (image != null) {
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        }

        filePath = getFullImageFilePath();
        if (filePath == null) {
            filePath = getCachedFilePath();
        }

        return BitmapFactory.decodeFile(filePath);
    }

    /***
     * Method to retrieve the image byte array
     * @return image byte array
     */
    public byte[] getImageBytes() {
        return image;
    }

    /***
     * Method to get boolean value indicating whether an image is selected or not
     * @return true if image is selected, else false
     */
    public boolean isSelected() {
        return selected;
    }

    /***
     * Setter method to set the selection state of an image
     * @param selected true if selected, else false.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /***
     * Getter method to retrieve the image File name
     * @return filename of the image
     */
    public String getImageFileName() {
        return filename;
    }


    /***
     * method to retrieve the path of the cached image
     * @return the filepath of the cached image file
     */
    public String getCachedFilePath() {
        return cachePath;
    }

    /***
     * Method to set the cache path of the image
     * @param path the path of the cached image
     */
    public void setCachePath(String path) {
        cachePath = path;
    }

    /***
     * getter method to get the image path of the full image
     * @return image path of the full/original image
     */
    public String getFullImageFilePath() {
        return filepath;
    }
}
