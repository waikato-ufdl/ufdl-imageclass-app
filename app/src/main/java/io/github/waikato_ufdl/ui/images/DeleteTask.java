package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.view.ActionMode;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import java.util.ArrayList;
import io.github.waikato_ufdl.ui.settings.Utility;

/**
 * A DeleteTask will be used to deleted selected images from the backend
 * */

public class DeleteTask extends NetworkTask {
    private ArrayList<ClassifiedImage> selectedImages;
    ArrayList<ClassifiedImage> storedImages;

    /**
     * The constructor for generating an UploadTask
     * @param fragment the ImagesFragment
     * @param context the context
     * @param images the list of selected images (ClassifiedImage objects)
     * @param datasetPK the primary key of the dataset that will be modified
     * @param action the action used to perform operations on ImageClassification datasets
     * @param mode the action mode
     */
    public DeleteTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, int datasetPK, ImageClassificationDatasets action, ActionMode mode) {
        super(fragment, context, datasetPK, action, mode);

        this.selectedImages = images;
        processingMessage = "Deleting image ";
        completedMessage = "Successfully deleted selected images";
        storedImages = Utility.getImageList(datasetPK);
    }

    /**
     *
     * @param image represents a classified image object
     * @throws Exception
     */
    @Override
    public void backgroundTask(Object image) throws Exception {
        action.deleteFile(datasetPK, ((ClassifiedImage) image).getImageFileName());
        storedImages.remove(image);
    }

    /**
     * will execute the deletion process and display a progress dialog to the user
     */
    @Override
    public void execute() {
        run(selectedImages);
    }

    /**
     * save the modified list to Utilities
     */
    @Override
    public void runOnCompletion() {
        Utility.saveImageList(datasetPK, storedImages);
        super.runOnCompletion();
    }
}
