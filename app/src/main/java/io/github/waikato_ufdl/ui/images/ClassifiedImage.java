package io.github.waikato_ufdl.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ClassifiedImage {
    private byte[] imageArray;
    private String classification;
    private boolean selected;
    private String imageFileName;

    /**
     * Constructor for a classified Image object
     * @param imgArray The byte array of the image
     * @param label The image's classification
     */
    public ClassifiedImage(byte[] imgArray, String label, String filename)
    {
        imageArray = imgArray;
        classification = label;
        selected = false;
        imageFileName = filename;
    }

    /**
     * Method to retrieve the label of the image
     * @return classification label string
     */
    public String getClassification() {
        return classification;
    }

    /**
     * Method to retrieve the image as a bitmap
     * @return image Bitmap
     */
    public Bitmap getImage()
    {
        return BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
    }

    /**
     * Method to retrieve the image array
     * @return
     */
    public byte[] getImageArray() {
        return imageArray;
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
        return imageFileName;
    }

    /**
     * Setter method for setting the classification label of an image
     * @param label
     */
    public void setClassificationLabel(String label)
    {
        classification = label;
    }
}
