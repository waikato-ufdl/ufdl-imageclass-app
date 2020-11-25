package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.net.Uri;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import io.github.waikato_ufdl.ui.UriUtils;

/**
 * An UploadTask will be used to upload images selected from the user's gallery to the backend
 */
public class UploadTask extends NetworkTask
{
    private final List<Uri> galleryImages;
    private String[] labels;
    private String label;
    private int index = 0;


    /**
     * The constructor for generating an UploadTask
     * @param fragment the ImagesFragment
     * @param context the context
     * @param galleryImages the list of URI's of images selected from gallery
     * @param labels the classification labels associated with the selected images
     * @param datasetPK the primary key of the dataset that will be modified
     * @param action the action used to perform operations on ImageClassification datasets
     */
    public UploadTask(ImagesFragment fragment, Context context, List<Uri> galleryImages, String[] labels, int datasetPK, ImageClassificationDatasets action) {
        super(fragment, context, datasetPK, action, null);

        processingMessage = "Uploading image ";
        completedMessage = "Successfully uploaded selected images";
        this.galleryImages = galleryImages;
        this.labels = labels;
    }

    /**
     * @param image either a ClassifiedImage object or URI object. In this case, it is treated as a URI object.
     * @throws Exception
     */
    @Override
    public void backgroundTask(Object image) throws Exception {
        label = labels[index];
        String classificationLabel = (label != null) ? label : "unlabelled";
        File imageFile = new File(UriUtils.getPathFromUri(context, ((Uri) image)));

        //add image file + label to the backend
        action.addFile(datasetPK, imageFile, imageFile.getName());
        action.addCategories(datasetPK, Arrays.asList(imageFile.getName()), Arrays.asList(classificationLabel));
        index++;
    }

    /**
     * will display a progress dialog whilst adding each image (along with its category) to the backend
     */
    @Override
    public void execute() {
        run(galleryImages);
    }


    /**
     * Method will set the retrievedAll variable to false so that the newly added images will be loaded from the backend
     */
    @Override
    public void runOnCompletion() {
        fragment.setRetrievedAll(false);
        super.runOnCompletion();
    }
}