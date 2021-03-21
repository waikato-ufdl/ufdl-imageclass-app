package io.github.waikato_ufdl.ui.images;

import android.content.Context;
import android.util.Log;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import io.github.waikato_ufdl.DatasetOperations;
import io.github.waikato_ufdl.SessionManager;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * A task to download the image contents of a dataset from the server
 */

public abstract class DownloadTask extends NetworkTask {
    private final boolean isCache;
    private final String[] imageFiles;
    private final File datasetFolder;
    private final ImageClassificationDatasets action;
    private int numDownloaded;

    /**
     * The constructor for generating a Download Task
     *
     * @param context     the context
     * @param datasetName the name of the dataset to download
     * @param isCache     whether to download cache or not
     */
    protected DownloadTask(Context context, String datasetName, boolean isCache) throws Exception {
        super(context, datasetName);
        this.isCache = isCache;
        this.datasetFolder = DatasetOperations.getImageStorageDirectory(context, datasetName, isCache);
        this.action = SessionManager.getClient().action(ImageClassificationDatasets.class);
        this.imageFiles = action.load(datasetPK).getFiles();
        this.numDownloaded = (int) dbManager.getSyncedImageCount(datasetName);
    }

    /***
     * Background task to download images from the server
     * @param fileName the name of the image file to download
     */
    @Override
    public void backgroundTask(Object fileName) {
        try {
            String filename = (String) fileName;
            byte[] image = action.getFile(datasetPK, filename);
            List<String> allLabels = action.getCategories(datasetPK, filename);
            String classificationLabel = (allLabels.size() > 0) ? allLabels.get(0) : "-";
            DatasetOperations.downloadImage(context, dbManager, datasetFolder, filename, image, classificationLabel, isCache);
        } catch (Exception e) {
            Log.e("TAG", "Failed to download image file: " + fileName + "\n" + e.getMessage());
        }
    }

    /***
     * Runs the task to download images from the server
     * @throws ImagesAlreadyExistException if the images have already been downloaded prior
     */
    @Override
    public void execute() throws ImagesAlreadyExistException {
        if (imageFiles.length != numDownloaded) run(Arrays.asList(imageFiles));
        else throw new ImagesAlreadyExistException("Images already downloaded for this dataset");
    }

    /***
     * Triggers the onPageLimitLoaded method if the number of downloaded images is less than the page limit defined in the images fragment.
     */
    @Override
    public void runOnNext() {
        numDownloaded++;
        if (numDownloaded <= ImagesFragment.PAGE_LIMIT) {
            onPageLimitLoaded();
        }
    }

    /***
     * Executes when the number of downloaded images is less than the page limit of the images fragment.
     */
    public abstract void onPageLimitLoaded();

    /***
     * Method which is called when the download task begins (overridden so that a loading dialog is not displayed)
     * @param disposable the disposable resource
     */
    @Override
    public void runOnSubscribe(Disposable disposable) {
    }


    static class ImagesAlreadyExistException extends Exception {
        public ImagesAlreadyExistException(String message) {
            super(message);
        }
    }
}
