package io.github.waikato_ufdl;

import android.util.Log;

import com.github.waikatoufdl.ufdl4j.action.ImageClassificationDatasets;

import java.io.File;
import java.util.Collections;
import java.util.List;

/***
 * Encapsulates image operations for uploading, updating and deleting images from the local database and the server.
 */

public class ImageOperations {
    /***
     * If an Internet connection is available, this method will upload an image directly to the server. Else, the image will be uploaded
     * to the local database with a sync status of CREATE.
     * @param dbManager the database manager
     * @param datasetPK the primary key of the dataset to store the image
     * @param dataset the name of the dataset to store the image
     * @param imageFile the image file to upload
     * @param label the classification label of the image
     */
    public static void uploadImage(DBManager dbManager, int datasetPK, String dataset, File imageFile, String label) {
        dbManager.insertUnsyncedImage(imageFile.getName(), label, imageFile.getAbsolutePath(), null, dataset);

        if (SessionManager.isOnlineMode) {
            ImageOperations.upload(dbManager, datasetPK, dataset, imageFile, label);
        }
    }


    /***
     * Performs an API request to upload an image to the server. Then sets the sync status of the image to SYNCED in the local database.
     * @param dbManager the database manager
     * @param datasetPK the primary key of the dataset to store the image
     * @param dataset the name of the dataset to store the image
     * @param imageFile the image file to upload
     * @param label the classification label of the image
     */
    public static void upload(DBManager dbManager, int datasetPK, String dataset, File imageFile, String label) {
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            //add the file to the server
            if (action.addFile(datasetPK, imageFile, imageFile.getName())) {
                //if the file has successfully been uploaded then update the image categories
                if (action.addCategories(datasetPK, Collections.singletonList(imageFile.getName()), Collections.singletonList(label))) {
                    //set the image sync status to synced on the local database
                    if (!dbManager.insertSyncedImage(imageFile.getName(), label, imageFile.getAbsolutePath(), null, dataset))
                        dbManager.setImageSynced(dataset, imageFile.getName());
                }
            }
        } catch (Exception e) {
            Log.e("TAG", "Failed to upload image");
        }
    }

    /***
     * Reclassifies an image on the local database and then sets the sync status to UPDATE. Then an API request will be performed to reclassify the image on the server. If the request
     * is successful, then the sync status of the image will be set to SYNCED in the local database.
     * @param dbManager the database manager
     * @param datasetPK the primary key of the dataset in which the image belongs
     * @param dataset the name of the dataset in which the image belongs
     * @param name the name of the image to update
     * @param oldLabel the old classification label of the image
     * @param newLabel the new classification label of the image
     */
    public static void updateImage(DBManager dbManager, int datasetPK, String dataset, String name, List<String> oldLabel, String newLabel) {
        dbManager.reclassifyImage(dataset, name, newLabel);

        if (SessionManager.isOnlineMode) {
            update(dbManager, datasetPK, dataset, name, oldLabel, newLabel);
        }
    }

    /***
     * Performs an API request to reclassify an image on the server. If the request is successful, the sync status of the image will be set to SYNCED in the local database.
     * @param dbManager the database manager
     * @param datasetPK the primary key of the dataset in which the image belongs
     * @param dataset the name of the dataset in which the image belongs
     * @param name the name of the image to update
     * @param oldLabel the old classification label of the image
     * @param newLabel the new classification label of the image
     */
    public static void update(DBManager dbManager, int datasetPK, String dataset, String name, List<String> oldLabel, String newLabel) {
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            //remove the old categories of the image and replace then with the new categories
            if (action.removeCategories(datasetPK, Collections.singletonList(name), oldLabel)) {
                if (action.addCategories(datasetPK, Collections.singletonList(name), Collections.singletonList(newLabel))) {
                    //set the image status to synced
                    dbManager.setImageSynced(dataset, name);
                }
            }
        } catch (Exception e) {
            Log.e("TAG", "API request failed -- Image Update Failed");
        }
    }

    /***
     * Sets the sync status of an image to DELETE on the local database and then an API request will be performed to delete the image on the server. If the request is successful,
     * the image record will be deleted from the local database aand the cached image will be deleted from the device.
     * @param dbManager the database manager
     * @param datasetPK the primary key of the dataset in which the image belongs
     * @param dataset the name of the dataset in which the image belongs
     * @param image the name of the image to delete
     * @param cachePath the cache path of the image to delete
     */
    public static void deleteImage(DBManager dbManager, int datasetPK, String dataset, String image, String cachePath) {
        //set the image sync status to delete
        dbManager.deleteImage(dataset, image);

        //if we are in online mode, attempt to delete the image file from the server
        if (SessionManager.isOnlineMode) {
            delete(dbManager, datasetPK, dataset, image, cachePath);
        }
    }

    /***
     * Performs an API request to delete an image from the server. If the request is successful, delete the image record from the local database and deletes the cached image
     * from the mobile device.
     * @param dbManager the database manager
     * @param datasetPK the primary key of the dataset in which the image belongs
     * @param dataset the name of the dataset in which the image belongs
     * @param image the name of the image to delete
     * @param cachePath the cache path of the image to delete
     */
    public static void delete(DBManager dbManager, int datasetPK, String dataset, String image, String cachePath) {
        try {
            ImageClassificationDatasets action = SessionManager.getClient().action(ImageClassificationDatasets.class);
            //if the image has been successfully deleted
            if (action.deleteFile(datasetPK, image)) {
                //set it's sync status to synced and then delete it's record from the local database
                dbManager.setImageSynced(dataset, image);
                String fullPath = dbManager.getImagePath(image, false);
                boolean removed = dbManager.deleteSyncedImage(dataset, image);

                //delete cached image
                if (removed && cachePath != null) {
                    File file = new File(cachePath);
                    if (file.exists()) file.delete();
                }

                //delete full image only if it is in the UFDL directory
                if (removed && fullPath != null)
                    if (fullPath.contains("UFDL") && fullPath.contains(dataset))
                        new File(fullPath).delete();

            }
        } catch (Exception e) {
            Log.e("TAG", "API Request Failed -- Image Deletion Failed\n" + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("Doesn't exist")) {
                dbManager.setImageSynced(dataset, image);
                dbManager.deleteSyncedImage(dataset, image);
            }
        }
    }
}
