package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A ReclassifyTask will be used to reclassify all selected images with a single given label
 */

public class ReclassifyTask extends NetworkTask {
    private ArrayList<ClassifiedImage> selectedImages;
    private String label;

    /**
     * The constructor for generating an UploadTask
     * @param fragment the ImagesFragment
     * @param context the context
     * @param images the list of images selected by the user (ClassifiedImage objects)
     * @param label the classification label to allocate to all selected images
     * @param datasetPK the primary key of the dataset that will be modified
     * @param action the action used to perform operations on ImageClassification datasets
     * @param mode the action mode
     */
    public ReclassifyTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, String label, int datasetPK, ImageClassificationDatasets action, ActionMode mode) {
        super(fragment, context, datasetPK, action, mode);

        this.selectedImages = images;
        this.label = label;
        processingMessage = "Reclassifying image ";
        completedMessage = "The selected images have been reclassified as: " + label;
    }

    /**
     * Method to reclassify images with the given classification label
     * @param image either a ClassifiedImage object or URI object
     * @throws Exception
     */
    @Override
    public void backgroundTask(Object image) throws Exception {
        //make an API request to remove current the categories for each image
        action.removeCategories(datasetPK, Arrays.asList(((ClassifiedImage) image).getImageFileName()), Arrays.asList(((ClassifiedImage) image).getClassification()));
        action.addCategories(datasetPK, Arrays.asList(((ClassifiedImage) image).getImageFileName()), Arrays.asList(label));
        //reclassify all the selected images locally with the user defined label
        ((ClassifiedImage) image).setClassificationLabel(label);
    }

    /**
     * execute the reclassification task on the selected images
     */
    @Override
    public void execute() {
        run(selectedImages);
    }

}
