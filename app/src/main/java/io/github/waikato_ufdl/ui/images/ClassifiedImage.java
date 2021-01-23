package io.github.waikato_ufdl.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import io.github.waikato_ufdl.DBManager;
import io.github.waikato_ufdl.ui.settings.Utility;

public class ClassifiedImage{
    private byte[] image;
    private String classification;
    private boolean selected;
    private String filename;
    private String filepath;
    private String cachePath;
    private String datasetName;
    private int syncStatus;


    /**
     * Constructor for a classified Image object
     * @param image The byte array of the image
     * @param label The image's classification
     */
    public ClassifiedImage(byte[] image, String label, String filename)
    {
        this.image = image;
        this.classification = label;
        this.filename = filename;
        selected = false;
    }

    /**
     * Alternative constructor for a classified Image object
     * @param filename the name of the image file
     * @param label the classification label
     * @param cachePath the filepath of the cached image
     */
    public ClassifiedImage(String filename, String label, String cachePath)
    {
        this.filename = filename;
        this.classification = label;
        this.cachePath = cachePath;
        selected = false;
    }

    public ClassifiedImage(String filename, String label, String filepath, String cachePath)
    {
        this(filename, label, cachePath);
        this.filepath = filepath;
    }


    public ClassifiedImage(String datasetName, String filename, String label, String filepath, int syncStatus)
    {
        this.datasetName = datasetName;
        this.filename = filename;
        this.classification = label;
        this.filepath = filepath;
        this.syncStatus = syncStatus;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public int getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(int syncOperation) {
        this.syncStatus = syncOperation;
    }

    /**
     * Method to retrieve the label of the image
     * @return classification label string
     */
    public String getClassificationLabel() {
        return classification;
    }

    /**
     * Method to retrieve the image as a bitmap
     * @return image Bitmap
     */
    public Bitmap getImageBitmap()
    {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    /**
     * Method to retrieve the image array
     * @return
     */
    public byte[] getImageBytes() {
        return image;
    }

    /**
     * Method to get boolean value indicating whether an image is selected or not
     * @return
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Setter method to set the state of an image : selected = true, unselected = false
     * @param selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Getter method to retrieve the image File name
     * @return
     */
    public String getImageFileName() {
        return filename;
    }

    /**
     * Setter method for setting the classification label of an image
     * @param label the classification label
     */
    public void setClassificationLabel(String label)
    {
        classification = label;
    }

    /**
     * method to retrieve the path of the cached image
     * @return the filepath of the cached image file
     */
    public String getCachedFilePath()
    {
        return cachePath;
    }

    public void setCachePath(String path)
    {
        cachePath = path;
    }

    public String getFullImageFilePath()
    {
        return filepath;
    }
}
