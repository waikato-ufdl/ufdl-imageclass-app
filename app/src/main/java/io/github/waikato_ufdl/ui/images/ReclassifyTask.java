package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.waikato_ufdl.ui.settings.Utility;

/**
 * A ReclassifyTask will be used to reclassify all selected images with a single given label
 */

public class ReclassifyTask extends NetworkTask {
    private ArrayList<ClassifiedImage> selectedImages;
    private String label;

    /**
     * The constructor for generating an UploadTask
     *
     * @param fragment  the ImagesFragment
     * @param context   the context
     * @param images    the list of images selected by the user (ClassifiedImage objects)
     * @param label     the classification label to allocate to all selected images
     * @param action    the action used to perform operations on ImageClassification datasets
     * @param mode      the action mode
     */
    public ReclassifyTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, String label, String datasetName, ImageClassificationDatasets action, ActionMode mode) {
        super(fragment, context, datasetName, action, mode);

        this.selectedImages = images;
        this.label = label;
        processingMessage = "Reclassifying image ";
        completedMessage = "The selected images have been reclassified as: " + label;
    }

    /**
     * Method to reclassify images with the given classification label
     *
     * @param image either a ClassifiedImage object or URI object
     * @throws Exception
     */
    @Override
    public void backgroundTask(Object image) throws Exception {

        String imageFileName = ((ClassifiedImage) image).getImageFileName();
        String classificationLabel = ((ClassifiedImage) image).getClassificationLabel();

        //update the sync status of the particular image to reclassi
        dbManager.reclassifyImage(datasetName, imageFileName, label);

        if (Utility.isOnlineMode) {
            //make an API request to remove current the categories for each image & reclassify the image using the new label
            if (action.removeCategories(datasetPK, Arrays.asList(imageFileName), Arrays.asList(classificationLabel)) &
                    (action.addCategories(datasetPK, Arrays.asList(imageFileName), Arrays.asList(label)))) {

                //update the image information in the local database
                dbManager.setImageSynced(datasetName, imageFileName);
            }
        }

        //reload the fragment after making the change to the local database
        //fragment.reload();
    }

    /**
     * execute the reclassification task on the selected images
     */
    @Override
    public void execute() {
        run(selectedImages);
    }
}
