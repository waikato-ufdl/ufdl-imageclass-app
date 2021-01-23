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
     * @param action the action used to perform operations on ImageClassification datasets
     * @param mode the action mode
     */
    public DeleteTask(ImagesFragment fragment, Context context, ArrayList<ClassifiedImage> images, String datasetName, ImageClassificationDatasets action, ActionMode mode) {
        super(fragment, context, datasetName, action, mode);

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
        String imageFilename = ((ClassifiedImage) image).getImageFileName();

        //set the image sync status to delete
        dbManager.deleteImage(datasetName, imageFilename);

        //if we are in online mode, attempt to delete the image file from the backend
        if(Utility.isOnlineMode) {

            //if the image has been successfully deleted
            if(action.deleteFile(datasetPK, imageFilename))
            {
                //set it's sync status of the image to synced and then delete it's record from the local database
                dbManager.setImageSynced(datasetName, imageFilename);
                dbManager.deleteSyncedImage(datasetName, imageFilename);
            }
        }

        /*
        else {
            fragment.reload();
        }
         */
    }

    /**
     * will execute the deletion process and display a progress dialog to the user
     */
    @Override
    public void execute() {
        run(selectedImages);
    }
}
