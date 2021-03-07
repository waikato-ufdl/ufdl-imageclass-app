package io.github.waikato_ufdl;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Simple Data object with two fields for the Prediction's label and probability
 */

public class Prediction implements Comparable<Prediction> {
    private final String label;
    private final Float confidence;

    /***
     * The constructor for the Prediction object
     * @param label the predicted label
     * @param confidence the confidence of the prediction
     */
    public Prediction(String label, Float confidence) {
        this.label = label;
        this.confidence = confidence;
    }

    /***
     * describes the prediction's details
     * @return formatted Prediction details
     */
    @NotNull
    @Override
    public String toString() {
        return "Predicted: " + label + "\nConfidence: " + getFormattedConfidence();
    }

    /***
     * Gets the predicted classification label
     * @return the predicted classification label
     */
    public String getLabel() {
        return label;
    }

    /***
     * Gets the prediction's confidence score
     * @return the confidence score of the prediction
     */
    public Float getConfidence() {
        return confidence;
    }

    /***
     * Get's the formatted confidence score in percentage format.
     * @return formatted confidence score in percentage format.
     */
    @SuppressLint("DefaultLocale")
    public String getFormattedConfidence() {
        return String.format("%.1f%%", confidence * 100.0f);
    }

    /***
     * The Diff-Util item call back to calculate the diff between two non-null items in a list
     */
    public static DiffUtil.ItemCallback<Prediction> itemCallback = new DiffUtil.ItemCallback<Prediction>() {
        /***
         * Called to check whether two objects represent the same item.
         * @param oldItem the item in the old list
         * @param newItem the item in the new list
         * @return True if the two items represent the same object or false if they are different.
         */
        @Override
        public boolean areItemsTheSame(@NonNull Prediction oldItem, @NonNull Prediction newItem) {
            return oldItem.label.equals(newItem.label);
        }

        /***
         * Called to check whether two items have the same data.
         * @param oldItem the item in the old list
         * @param newItem the item in the new list
         * @return True if the contents of the items are the same or false if they are different.
         */
        @Override
        public boolean areContentsTheSame(@NonNull Prediction oldItem, @NonNull Prediction newItem) {
            return oldItem.confidence.equals(newItem.confidence);
        }
    };

    /***
     * Compares a prediction object to another prediction object
     * @param recognition the prediction object to compare
     * @return Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(Prediction recognition) {
        return Float.compare(recognition.confidence, this.confidence);
    }
}