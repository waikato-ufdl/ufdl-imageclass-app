package io.github.waikato_ufdl;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.waikatoufdl.ufdl4j.action.Datasets;
import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import id.zelory.compressor.Compressor;
import io.github.waikato_ufdl.ui.manage.ImageDataset;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/***
 * Encapsulates dataset operations for creating, updating and deleting datasets from both the local and backend databases.
 */

public class DatasetOperations {
    /***
     * Inserts a image dataset to the local SQLite database with a sync status of CREATE and then attempts to make an API request to create the dataset on the backend.
     * @param dbManager the local database manager
     * @param name the name of the dataset
     * @param description the dataset description
     * @param project the project pk which the dataset belongs to
     * @param license the license pk of the dataset
     * @param isPublic whether the dataset is public
     * @param tags the dataset's tags
     */
    public static void createDataset(DBManager dbManager, String name, String description, int project, int license, boolean isPublic, String tags) {
        dbManager.insertLocalDataset(name, description, project, license, isPublic, tags);
        if (SessionManager.isOnlineMode)
            create(dbManager, name, description, project, license, isPublic, tags);
    }

    /***
     * Performs an API request to create a dataset on the backend. If the request is successful, the dataset will be set to SYNCED in the local database.
     * @param dbManager the local database manager
     * @param name the name of the dataset
     * @param description the dataset description
     * @param project the project pk which the dataset belongs to
     * @param license the license pk of the dataset
     * @param isPublic whether the dataset is public
     * @param tags the dataset's tags
     */
    public static void create(DBManager dbManager, String name, String description, int project, int license, boolean isPublic, String tags) {
        Datasets.Dataset dataset;
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            if ((dataset = action.create(name, description, project, license, isPublic, tags)) != null) {
                dbManager.setDatasetSynced(dataset.getName());
                dbManager.updateLocalDatasetAfterSync(dataset.getPK(), dataset.getName(), dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags());
            }
        } catch (Exception e) {
            Log.e("TAG", "Error while trying to upload database:\n" + e.getMessage());
        }
    }


    /***
     * Updates the dataset's information in the local database and sets its sync status to UPDATE and then attempts to perform an API request to update the dataset on the backend.
     * @param dbManager the local database manager
     * @param datasetPK the primary key of the dataset
     * @param name the new name of the dataset
     * @param oldDatasetName the current name of the dataset
     * @param description the dataset description
     * @param project the project pk which the dataset belongs to
     * @param license the license pk of the dataset
     * @param isPublic whether the dataset is public
     * @param tags the dataset's tags
     */
    public static void updateDataset(DBManager dbManager, int datasetPK, String name, String oldDatasetName, String description, int project, int license, boolean isPublic, String tags) {
        dbManager.updateDataset(oldDatasetName, name, description, project, license, isPublic, tags);

        if (SessionManager.isOnlineMode)
            update(dbManager, datasetPK, name, description, project, license, isPublic, tags);

    }

    /***
     * Performs an API request to update an existing dataset on the backend. If the request is successful, the dataset will be set to SYNCED in the local database.
     * @param dbManager the local database manager
     * @param datasetPK the primary key of the dataset
     * @param name the new name of the dataset
     * @param description the dataset description
     * @param project the project pk which the dataset belongs to
     * @param license the license pk of the dataset
     * @param isPublic whether the dataset is public
     * @param tags the dataset's tags
     */
    public static void update(DBManager dbManager, int datasetPK, String name, String description, int project, int license, boolean isPublic, String tags) {
        Datasets.Dataset dataset;
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            if ((dataset = action.update(datasetPK, name, description, project, license, isPublic, tags)) != null) {
                dbManager.setDatasetSynced(dataset.getName());
            }
        } catch (Exception e) {
            Log.e("TAG", "API request failed -- Dataset Update Failed. Error:\n" + e.getMessage());
        }
    }

    /***
     * Sets the sync status of a dataset to DELETE in the local database and then attempts to perform an API request to delete the dataset from the backend.
     * @param context the context
     * @param dbManager the local database manager
     * @param name the name of the dataset
     */
    public static void deleteDataset(Context context, DBManager dbManager, String name) {
        dbManager.deleteDataset(name);

        if (SessionManager.isOnlineMode)
            delete(context, dbManager, name);

    }

    /***
     * Performs an API request to attempt to delete a dataset from the backend. If the request is successful, the dataset will be removed from the local database.
     * @param dbManager the local database manager
     * @param name the name of the dataset
     */
    public static void delete(Context context, DBManager dbManager, String name) {
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            int datasetPK = dbManager.getDatasetPK(name);

            if (action.delete(datasetPK, true)) {
                dbManager.setDatasetSynced(name);
                dbManager.deleteLocalDataset(name);
                deleteDatasetStorageDirectories(context, name);
            }
        } catch (Exception e) {
            Log.e("TAG", "API request failed -- Dataset Deletion Failed. Error:\n" + e.getMessage());

        }
    }

    /***
     * Performs an API request to make a copy of an existing dataset.
     * @param dbManager the local database manager
     * @param dataset the name of the dataset to copy
     * @param newDatasetName the name to give the copied dataset
     * @return true if the dataset copy has been successfully created or false if the copy operation has failed.
     */
    public static boolean copyDataset(DBManager dbManager, ImageDataset dataset, String newDatasetName) {
        try {
            if (SessionManager.isOnlineMode) {
                ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
                //create a copy of the dataset
                if (action.copy(dataset.getPK(), newDatasetName)) {
                    int pk = action.load(newDatasetName).getPK();
                    dbManager.insertSyncedDataset(pk, newDatasetName, dataset.getDescription(), dataset.getProject(), dataset.getLicense(), dataset.isPublic(), dataset.getTags());
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e("TAG", "API request failed -- Dataset Copy Failed. Error: \n" + e.getMessage());
        }
        return false;
    }

    /***
     * Downloads the contents (images) of a dataset
     * @param context the context
     * @param dbManager the local database manager
     * @param selectedDataset the dataset to download
     * @param isCache true to compress images
     * @param progressBar the progress bar to display
     */
    public static void downloadDataset(Context context, DBManager dbManager, ImageDataset selectedDataset, boolean isCache, ProgressBar progressBar, Button cancelButton) {
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            int datasetPK = selectedDataset.getPK();
            String datasetName = selectedDataset.getName();
            String[] imageFileNames = action.load(datasetPK).getFiles();
            File datasetFolder = getImageStorageDirectory(context, datasetName, isCache);

            Observable.fromArray(imageFileNames)
                    .map(filename -> {
                        byte[] image = action.getFile(datasetPK, filename);
                        List<String> allLabels = action.getCategories(datasetPK, filename);
                        String classificationLabel = (allLabels.size() > 0) ? allLabels.get(0) : "Unlabelled";

                        return downloadImage(context, dbManager, datasetFolder, filename, image, classificationLabel, isCache);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(getDownloadObserver(progressBar, cancelButton, imageFileNames.length));

        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(() ->
            {
                progressBar.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                Toast.makeText(context, "Error occurred while downloading datasets: \n +" + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }


    /***
     * Downloads an image and stores it within an app-specific directory.
     * @param context the context
     * @param dbManager the local database manager
     * @param datasetFolder the folder to store the image in
     * @param filename the name of the image file
     * @param image the image data
     * @param classificationLabel the classification label of the image
     * @param isCache whether the file is cache file or not
     * @return true if image has been downloaded and stored or else false if an error occurred.
     */
    public static boolean downloadImage(Context context, DBManager dbManager, File datasetFolder, String filename, byte[] image, String classificationLabel, boolean isCache) {
        try {
            File file = new File(datasetFolder, filename);

            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file.getPath());
                fos.write(image);
                fos.close();
            }

            if (isCache) {
                compressFile(context, file);
                if (!dbManager.insertSyncedImage(filename, classificationLabel, null, file.getPath(), datasetFolder.getName()))
                    dbManager.insertImagePath(filename, file.getPath(), true);

            } else {
                if (!dbManager.insertSyncedImage(filename, classificationLabel, file.getPath(), null, datasetFolder.getName()))
                    dbManager.insertImagePath(filename, file.getPath(), false);
            }

            return true;
        } catch (Exception e) {
            Log.e("TAG", "Error: " + e.getMessage());
        }

        return false;
    }

    /***
     * Gets the directory to store a particular dataset's full images or its image cache.
     * @param context the context
     * @param dataset the name of the dataset (name to give subdirectory)
     * @param isCache true if the folder should be a subdirectory of the cache folder
     * @return the folder to store the images or null if the folder couldn't be created.
     */
    public static File getImageStorageDirectory(Context context, String dataset, boolean isCache) {
        int userPK = new SessionManager(context).getUserPK();
        String mainFolderName = (isCache) ? "UFDL_CACHE" : "UFDL";
        File folder = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), mainFolderName);

        //create main directory if it doesn't exist
        if (!folder.exists()) {
            if (folder.mkdirs()) Log.e("TAG", "Created: " + mainFolderName);
        }

        //create dataset subdirectory
        File datasetFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + mainFolderName + File.separator + userPK, dataset);
        if (!datasetFolder.exists()) {
            if (datasetFolder.mkdirs()) return datasetFolder;
        } else
            return datasetFolder;

        return null;
    }

    /***
     * Gets the observer to set for the download dataset observable.
     * @param progressBar the progress bar to display
     * @param numFiles the number of files to download
     * @return RxJava observer
     */
    private static Observer<Boolean> getDownloadObserver(ProgressBar progressBar, Button cancelButton, int numFiles) {
        return new Observer<Boolean>() {
            int currentProgress = 1;

            @Override
            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setMax(numFiles);
                    progressBar.setSecondaryProgress(numFiles);
                    progressBar.setProgress(currentProgress);
                    cancelButton.setVisibility(View.VISIBLE);
                    cancelButton.setOnClickListener(view -> {
                        d.dispose();
                        onComplete();
                    });
                });
            }

            @Override
            public void onNext(@io.reactivex.rxjava3.annotations.NonNull Boolean aBoolean) {
                new Handler(Looper.getMainLooper()).post(() -> progressBar.setProgress(currentProgress++));
            }

            @Override
            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                Log.e("TAG", "Error when trying to download image:\n" + e.getMessage());
            }

            @Override
            public void onComplete() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    progressBar.setVisibility(View.GONE);
                    cancelButton.setVisibility(View.GONE);
                });
            }
        };
    }

    /***
     * Compresses an image file.
     * @param context the context
     * @param file the file to compress
     */
    protected static void compressFile(Context context, File file) {
        //create a compressed version of the image file
        Compressor compressor = new Compressor(context);

        try {
            File compressedImage = compressor.compressToFile(file);
            Log.e("TAG", compressedImage.getPath());

            //replace original file with compressed file
            Files.move(compressedImage.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            //delete the parent folder of where the compressed image was initially stored
            File parentFolder = compressedImage.getParentFile();
            if (parentFolder != null && parentFolder.exists()) {
                if (parentFolder.delete()) Log.e("TAG", "Successfully deleted parent folder");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * Delete the full images and image cache of the dataset.
     * @param context the context
     * @param dataset the name of the dataset
     * @throws IOException if deletion is unsuccessful
     */
    public static void deleteDatasetStorageDirectories(Context context, String dataset) throws IOException {
        int userPK = new SessionManager(context).getUserPK();
        File cacheFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + "UFDL_CACHE" + File.separator + userPK, dataset);
        File imageFolder = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + File.separator + "UFDL" + File.separator + userPK, dataset);
        File parent;

        if (cacheFolder.exists() && (parent = cacheFolder.getParentFile()).list().length == 1)
            FileUtils.deleteDirectory(parent);
        else FileUtils.deleteDirectory(cacheFolder);

        if (imageFolder.exists() && (parent = imageFolder.getParentFile()).list().length == 1)
            FileUtils.deleteDirectory(parent);
        else FileUtils.deleteDirectory(imageFolder);
    }
}
