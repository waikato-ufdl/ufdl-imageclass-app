package com.example.myapplication.ui.images;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ClassifiedImage {
    private byte[] imageArray;
    private String classification;

    /**
     * Constructor for a classified Image object
     * @param imgArray The byte array of the image
     * @param label The image's classification
     */
    public ClassifiedImage(byte[] imgArray, String label)
    {
        imageArray = imgArray;
        classification = label;
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
}
