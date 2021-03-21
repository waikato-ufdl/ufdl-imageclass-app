package io.github.waikato_ufdl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.waikato_ufdl.ui.images.ClassifiedImage;
import io.github.waikato_ufdl.ui.manage.ImageDataset;

public class DBManager {
    SessionManager sessionManager;
    private DatabaseHelper dbHelper;
    private final Context context;
    private SQLiteDatabase database;

    private final String TAG = "TAG";
    private final int DEFAULT_DATASET_PK = -1;
    private final int SYNCED = 0;
    private final int CREATE = 1;
    private final int UPDATE = 2;
    private final int DELETE = 3;

    public DBManager(Context c) {
        context = c;
    }

    /*** opens the database connection */
    public synchronized void open() {
        if (dbHelper == null) {
            sessionManager = new SessionManager(context);
            dbHelper = DatabaseHelper.getInstance(context);
            database = dbHelper.getWritableDatabase();

            //enable foreign keys
            database.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    /*** closes the database connection */
    public void close() {
        dbHelper.close();
    }

    /***
     * Gets the list of licenses stored on the database
     * @return list of licenses
     */
    public List<String> getLicenses() {
        List<String> licenses = new ArrayList<>();
        open();

        //query to return cursor object containing license data
        Cursor cursor = database.query(DatabaseHelper.TABLE_LICENSE, new String[]{DatabaseHelper.LICENSE_COL_NAME}, null, null, null, null, null, null);

        //if the cursor is not empty
        if (cursor.moveToFirst()) {

            //add license name to licenses list
            do {
                licenses.add(cursor.getString(0));
            }
            //while there is still data to retrieve
            while (cursor.moveToNext());
        }

        //close the cursor and the database connection
        cursor.close();
        return licenses;
    }


    /***
     * Store user login details in the local database
     * @param pk the primary key of the user
     * @param serverURL the server URL
     * @param username the username
     * @param password the password
     */
    public void storeUserDetails(int pk, String serverURL, String username, String password) {
        //open database connection
        open();
        ContentValues contentValues = new ContentValues();

        contentValues.put(DatabaseHelper.USER_PK, pk);
        contentValues.put(DatabaseHelper.USER_SERVER, serverURL);
        contentValues.put(DatabaseHelper.USER_USERNAME, username);
        contentValues.put(DatabaseHelper.USER_PASSWORD, password);

        //insert the content values into the user table
        database.insert(DatabaseHelper.TABLE_USER, null, contentValues);
    }

    /***
     * Gets the primary key of a user
     * @param serverURL the server URL
     * @param username the username
     * @param password the password
     * @return the primary key of the user or -1 if no primary key is found.
     */
    public int getUserPK(String serverURL, String username, String password) {
        open();
        int result;

        Cursor cursor = database.rawQuery("SELECT " + DatabaseHelper.USER_PK + " FROM " + DatabaseHelper.TABLE_USER + " WHERE " + DatabaseHelper.USER_SERVER + " LIKE ? AND "
                        + DatabaseHelper.USER_USERNAME + " LIKE ? AND "
                        + DatabaseHelper.USER_PASSWORD + " LIKE ?",
                new String[]{serverURL, username, password});

        //check if the cursor contains data
        if (cursor.moveToFirst()) result = cursor.getInt(0);
        else result = -1;

        //close cursor and database connection
        cursor.close();
        return result;
    }

    /***
     * Gets the list of servers stored in the database
     * @return list of servers
     */
    public ArrayList<String> getServers() {
        open();
        ArrayList<String> serverList = new ArrayList<>();

        //query to get server URLS
        Cursor cursor = database.query(DatabaseHelper.TABLE_USER, new String[]{DatabaseHelper.USER_SERVER}, null, null, null, null, null, null);

        //check if cursor contains any data
        if (cursor.moveToFirst()) {
            do {
                //add server URL to list
                serverList.add(cursor.getString(0));
            }
            //while there is more server URLs
            while (cursor.moveToNext());
        }

        cursor.close();
        return serverList;
    }

    /***
     * Gets the list of usernames stored in the local database
     * @return list of usernames
     */
    public ArrayList<String> getUsernames() {
        open();
        ArrayList<String> userList = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_USER, new String[]{DatabaseHelper.USER_USERNAME}, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                userList.add(cursor.getString(0));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return userList;
    }


    /***
     * Get the primary key of a license
     * @param licenseName the name of the license
     * @return the primary key of a license or -1 if no key is found.
     */
    public int getLicenseKey(String licenseName) {
        open();
        int result;

        //query to get the primary key of a license given the name of the particular license (character case shouldn't affect retrieval)
        Cursor cursor = database.rawQuery("SELECT " + DatabaseHelper.LICENSE_COL_PK + " FROM " + DatabaseHelper.TABLE_LICENSE + " WHERE " + DatabaseHelper.LICENSE_COL_NAME + " = '" + licenseName + "' COLLATE NOCASE", null);

        //check to see if the cursor contains any data
        if (cursor.moveToFirst()) {
            //get license key
            result = cursor.getInt(0);
        } else {
            result = -1;
            Log.e("TAG", "License Key not found given license name = " + licenseName);
        }
        cursor.close();
        return result;
    }

    /***
     * Stores a license in the local database
     * @param licenseKey the primary key of the license
     * @param licenseName the name of the license
     */
    public void insertLicense(int licenseKey, String licenseName) {
        open();
        ContentValues contentValues = new ContentValues();

        //put the license key and name into the content values
        contentValues.put(DatabaseHelper.LICENSE_COL_PK, licenseKey);
        contentValues.put(DatabaseHelper.LICENSE_COL_NAME, licenseName);

        //insert the content values into the license table
        long result = database.insert(DatabaseHelper.TABLE_LICENSE, null, contentValues);
        if (result == -1)
            Log.e(TAG, String.format("Failed to insert license: [PK = %d, Name = %s]", licenseKey, licenseName));
    }

    /***
     * Gets the name of a license
     * @param licenseKey the primary key of the license
     * @return the name of the license or null if no license found.
     */
    public String getLicenseName(int licenseKey) {
        open();
        String result;

        //execute query to retrieve a license name given the primary key of the license
        Cursor cursor = database.rawQuery("SELECT " + DatabaseHelper.LICENSE_COL_NAME + " FROM " + DatabaseHelper.TABLE_LICENSE + " WHERE " + DatabaseHelper.LICENSE_COL_PK + " = " + licenseKey + "", null);

        //check to see if cursor contains any data
        if (cursor.moveToFirst()) {
            //get the license name
            result = cursor.getString(cursor.getColumnIndex(DatabaseHelper.LICENSE_COL_NAME));
        } else {
            result = null;
            Log.e(TAG, "License name not found given PK = " + licenseKey);
        }

        cursor.close();
        return result;
    }

    /***
     * Gets the list of projects stored in the database
     * @return the list of projects
     */
    public List<String> getProjects() {
        List<String> projects = new ArrayList<>();
        open();

        //query to get project names
        Cursor cursor = database.query(DatabaseHelper.TABLE_PROJECT, new String[]{DatabaseHelper.PROJECT_COL_NAME}, null, null, null, null, null, null);

        //check if cursor contains any data
        if (cursor.moveToFirst()) {
            do {
                //add project names to projects list
                projects.add(cursor.getString(0));
            }
            //while there is more project names
            while (cursor.moveToNext());
        }

        cursor.close();
        return projects;
    }

    /***
     * Gets the primary key of a project
     * @param projectName the name of the project
     * @return the primary key of the project
     */
    public int getProjectKey(String projectName) {
        open();
        int result;

        //query to retrieve project key given the name of the project
        Cursor cursor = database.rawQuery("SELECT " + DatabaseHelper.PROJECT_COL_PK + " FROM " + DatabaseHelper.TABLE_PROJECT + " WHERE " + DatabaseHelper.PROJECT_COL_NAME + " = '" + projectName + "' COLLATE NOCASE", null);

        //check if cursor contains any data
        if (cursor.moveToFirst()) {
            //get project key
            result = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.PROJECT_COL_PK));
        } else {
            //no project key found
            result = -1;
            Log.e(TAG, "No project PK found given project name = " + projectName);
        }

        cursor.close();
        return result;
    }

    /***
     * Gets the name of a project
     * @param projectKey the primary key of the project
     * @return the name of the project
     */
    public String getProjectName(int projectKey) {
        open();
        String result;

        //get the project name associated with a given project key
        Cursor cursor = database.rawQuery("SELECT " + DatabaseHelper.PROJECT_COL_NAME + " FROM " + DatabaseHelper.TABLE_PROJECT + " WHERE " + DatabaseHelper.PROJECT_COL_PK + " = " + projectKey + "", null);

        //check if the cursor contains data
        if (cursor.moveToFirst()) {
            //retrieve the project name
            result = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PROJECT_COL_NAME));
        } else {
            //no project name associated with the given key
            result = null;
            Log.e(TAG, "No project name found given project key =" + projectKey);
        }

        cursor.close();
        return result;
    }

    /***
     * Stores a project in the database
     * @param projectKey the primary key of the project to store
     * @param projectName the name of the project to store
     */
    public void insertProject(int projectKey, String projectName) {
        open();
        ContentValues contentValues = new ContentValues();

        //insert project key and project name into content values
        contentValues.put(DatabaseHelper.PROJECT_COL_PK, projectKey);
        contentValues.put(DatabaseHelper.PROJECT_COL_NAME, projectName);

        //insert the content values into the project table
        long result = database.insert(DatabaseHelper.TABLE_PROJECT, null, contentValues);
        if (result == -1)
            Log.e(TAG, String.format("Failed to insert project: [PK = %d, Name = %s]", projectKey, projectName));
    }

    /***
     * Gets the list of known datasets for the current user
     * @return the current user's datasets
     */
    public ArrayList<ImageDataset> getDatasetList() {
        open();
        ArrayList<ImageDataset> datasetList = new ArrayList<>();
        ImageDataset dataset;

        String[] columns = new String[]{
                DatabaseHelper.DS_COL_PK, DatabaseHelper.DS_COL_NAME, DatabaseHelper.DS_COL_DESCRIPTION, DatabaseHelper.DS_COL_PROJECT,
                DatabaseHelper.DS_COL_LICENSE, DatabaseHelper.DS_COL_PUBLIC, DatabaseHelper.DS_COL_TAGS, DatabaseHelper.DS_COL_SYNC_OPS};

        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, columns, DatabaseHelper.DS_COL_SYNC_OPS + " != ? AND " +
                DatabaseHelper.DS_COL_USER_PK + " = ?", new String[]{Integer.toString(DELETE), Integer.toString(sessionManager.getUserPK())}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                dataset = new ImageDataset(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5) == 1, cursor.getString(6), cursor.getInt(7));

                datasetList.add(dataset);
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return datasetList;
    }

    /***
     * Inserts a local dataset into the database with a sync status of CREATE. A local dataset is one which hasn't been created on the server and needs to be synced. Hence, it's primary
     * key will be -1 until it has been synced.
     * @param name the name of the dataset
     * @param description the description of the dataset
     * @param project the PK of the project that the dataset belongs to
     * @param license the PK of the license for the dataset
     * @param isPublic whether the dataset is public or not
     * @param tags the tags for the dataset
     */
    public void insertLocalDataset(String name, String description, int project, int license, boolean isPublic, String tags) {
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_NAME, name);
        contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
        contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
        contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
        contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
        contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);
        contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, CREATE);
        contentValues.put(DatabaseHelper.DS_COL_USER_PK, sessionManager.getUserPK());

        long result;
        try {
            result = database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);
            if (result == -1) Log.e(TAG, "Error: Failed to insert local dataset named: " + name);
        } catch (SQLException e) {
            Log.e(TAG, "Error: Failed to insert local dataset named:" + name + "\n" + e.getMessage());
        }
    }

    /***
     * Updates the information of a local dataset but doesn't change the sync status (it will remain at CREATE). A local dataset is one which hasn't been created on the server and needs
     * to be synced.
     * @param oldName the current name of the dataset
     * @param newName the new name to give to the dataset
     * @param description the description
     * @param project the PK of the project the dataset belongs to
     * @param license the PK of the license of the dataset
     * @param isPublic whether the dataset is public or not
     * @param tags the tags of the dataset
     */
    public void updateLocalDataset(String oldName, String newName, String description, int project, int license, boolean isPublic, String tags) {
        open();

        try {
            //disable foreign keys
            database.execSQL("PRAGMA foreign_keys = ON;");

            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.DS_COL_NAME, newName);
            contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
            contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
            contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
            contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
            contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);

            int result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " = ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " = ?  AND " + DatabaseHelper.DS_COL_USER_PK
                            + " = ?", new String[]{oldName, Integer.toString(CREATE), Integer.toString(sessionManager.getUserPK())});

            if (result == 0) Log.d(TAG, "Failed to update local dataset named: " + oldName);

        } catch (SQLException e) {
            Log.e(TAG, "Error occurred when trying to update local dataset named: " + oldName + "\n" + e.getMessage());
        }
    }

    /***
     * Updates a local dataset after it has been synced (created on the server).
     * @param datasetPk the primary key of the synced dataset
     * @param name the name of the dataset to update
     * @param description the description of the dataset
     * @param project the PK of the project the dataset belongs to
     * @param license the PK of the license of the dataset
     * @param isPublic whether the dataset is public or not
     * @param tags the tags of the dataset
     */
    public void updateLocalDatasetAfterSync(int datasetPk, String name, String description, int project, int license, boolean isPublic, String tags) {
        open();
        try {
            //disable foreign keys
            database.execSQL("PRAGMA foreign_keys = ON;");

            //add all the  values to content values
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.DS_COL_PK, datasetPk);
            contentValues.put(DatabaseHelper.DS_COL_NAME, name);
            contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
            contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
            contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
            contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
            contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);

            int result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " = ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " = ? AND " +
                            DatabaseHelper.DS_COL_USER_PK + " = ?", new String[]{name, Integer.toString(SYNCED), Integer.toString(sessionManager.getUserPK())});

            if (result == -1)
                Log.e(TAG, "Failed to update local dataset named : " + name + " after sync");

        } catch (SQLException e) {
            Log.e(TAG, "Error occurred while trying to update dataset named : " + name + " after sync\n" + e.getMessage());
        }
    }

    /***
     * Deletes a dataset from the local database if and only if the dataset is local (it doesn't exist on the server yet) or its sync status is SYNCED.
     * @param datasetName the name of the dataset to delete
     */
    public void deleteLocalDataset(String datasetName) {
        open();

        int result = database.delete(DatabaseHelper.TABLE_DATASET,
                DatabaseHelper.DS_COL_NAME + " LIKE ? AND (" + DatabaseHelper.DS_COL_PK + " LIKE ? OR "
                        + DatabaseHelper.DS_COL_SYNC_OPS + " = ?) AND " + DatabaseHelper.DS_COL_USER_PK + " = ?",
                new String[]{datasetName, Integer.toString(DEFAULT_DATASET_PK), Integer.toString(SYNCED), Integer.toString(sessionManager.getUserPK())});

        if (result == -1)
            Log.e(TAG, String.format("Failed to delete the dataset named %s from the local database", datasetName));
    }

    /***
     * Insert a synced dataset into the local database. A synced dataset is one which already exists on the server and hence, has a primary key.
     * @param datasetPK the primary key of the dataset
     * @param name the name of the dataset
     * @param description the desription of the dataset
     * @param project the primary key of the project the dataset belongs to
     * @param license the primary key of the license of the datset
     * @param isPublic whether the dataset is public or not
     * @param tags the tags of the dataset
     */
    public void insertSyncedDataset(int datasetPK, String name, String description, int project, int license, boolean isPublic, String tags) {
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_PK, datasetPK);
        contentValues.put(DatabaseHelper.DS_COL_NAME, name);
        contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
        contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
        contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
        contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
        contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);
        contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, SYNCED);
        contentValues.put(DatabaseHelper.DS_COL_USER_PK, sessionManager.getUserPK());

        try {
            long result = database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);
            if (result == -1) Log.e(TAG, "Failed to insert a synced dataset named " + name);
        } catch (SQLException e) {
            Log.e(TAG, "Failed to insert a synced dataset named " + name + "\n" + e.getMessage());
        }
    }

    /***
     * Updates the information of both local and synced datasets.
     * @param oldName the current name of the dataset
     * @param newName the new name of the dataset
     * @param description the description of the dataset
     * @param project the primary key of the project the dataset belongs to
     * @param license the primary key of the license of the datset
     * @param isPublic whether the dataset is public or not
     * @param tags the tags of the dataset
     */
    public void updateDataset(String oldName, String newName, String description, int project, int license, boolean isPublic, String tags) {
        open();

        try {
            //disable foreign keys
            database.execSQL("PRAGMA foreign_keys = ON;");

            //add all the  values to content values
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.DS_COL_NAME, newName);
            contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
            contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
            contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
            contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
            contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);
            contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, UPDATE);
            int result;

            //update the dataset ONLY if its sync status is not set to DELETE or CREATE
            result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " LIKE ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " NOT IN (?,?) AND "
                            + DatabaseHelper.DS_COL_USER_PK + " = ?", new String[]{oldName, Integer.toString(DELETE), Integer.toString(CREATE), Integer.toString(sessionManager.getUserPK())});

            if (result > 0) return;

            //if the sync status of the dataset is set to create, the dataset is still local and so call updateLocalDataset
            if (result == 0 && getDatasetSyncOpStatus(oldName) == CREATE) {
                updateLocalDataset(oldName, newName, description, project, license, isPublic, tags);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error occurred while trying to update a dataset named: " + oldName + "\n" + e.getMessage());
        }
    }


    /***
     * Sets the sync status of an already synced dataset (exists on server) to DELETE to await deletion. If the dataset is a local one (doesn't exist on server yet),
     * the deleteLocalDataset method will be called.
     * @param datasetName the name of the dataset to delete.
     */
    public void deleteDataset(String datasetName) {
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, DELETE);
        int result;

        //change the SYNC_OP of the dataset to delete if it's a synced dataset (has a dataset primary key != -1)
        result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                DatabaseHelper.DS_COL_PK + " != ? AND " + DatabaseHelper.DS_COL_NAME + " LIKE ? AND " + DatabaseHelper.DS_COL_USER_PK + " = ?",
                new String[]{Integer.toString(DEFAULT_DATASET_PK), datasetName, Integer.toString(sessionManager.getUserPK())});

        if (result == 0) deleteLocalDataset(datasetName);
    }


    /***
     * Gets the sync status of a dataset
     * @param datasetName the name of the dataset
     * @return an integer representing the sync status where: 0 = SYNCED, 1 = CREATE, 2 = UPDATE, 3 = DELETE
     */
    public int getDatasetSyncOpStatus(String datasetName) {
        open();
        int status = -1;
        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, new String[]{DatabaseHelper.DS_COL_SYNC_OPS},
                DatabaseHelper.DS_COL_NAME + " = ? AND " + DatabaseHelper.DS_COL_USER_PK + " = ?",
                new String[]{datasetName, Integer.toString(sessionManager.getUserPK())}, null, null, null, null);

        if (cursor.moveToFirst()) {
            status = cursor.getInt(0);
        } else Log.e(TAG, "Failed to get the sync status of the dataset named: " + datasetName);

        cursor.close();
        return status;

    }

    /***
     * Sets the sync status of a dataset to SYNCED
     * @param datasetName the name of the dataset for which the sync status needs to be changed
     */
    public void setDatasetSynced(String datasetName) {
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, SYNCED);

        int result = database.update(DatabaseHelper.TABLE_DATASET, contentValues, DatabaseHelper.DS_COL_NAME + " LIKE ? " +
                "AND " + DatabaseHelper.DS_COL_USER_PK + " = ?", new String[]{datasetName, Integer.toString(sessionManager.getUserPK())});
        if (result == 0)
            Log.e(TAG, "Failed to set the sync status of " + datasetName + "to SYNCED");
    }

    /***
     * Gets the list of all datasets waiting to be synced
     * @return the list of all unsynced datasets
     */
    public ArrayList<ImageDataset> getUnsyncedDatasets() {
        open();
        ArrayList<ImageDataset> datasetList = new ArrayList<>();
        ImageDataset dataset;

        String[] columns = new String[]{
                DatabaseHelper.DS_COL_PK, DatabaseHelper.DS_COL_NAME, DatabaseHelper.DS_COL_DESCRIPTION, DatabaseHelper.DS_COL_PROJECT,
                DatabaseHelper.DS_COL_LICENSE, DatabaseHelper.DS_COL_PUBLIC, DatabaseHelper.DS_COL_TAGS, DatabaseHelper.DS_COL_SYNC_OPS};

        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, columns,
                DatabaseHelper.DS_COL_SYNC_OPS + " != ? AND " + DatabaseHelper.DS_COL_USER_PK + " = ?",
                new String[]{Integer.toString(SYNCED), Integer.toString(sessionManager.getUserPK())}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                dataset = new ImageDataset(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5) == 1, cursor.getString(6), cursor.getInt(7));

                datasetList.add(dataset);
            }
            while (cursor.moveToNext());
        }

        cursor.close();
        return datasetList;
    }


    /***
     * Inserts a synced image to the local database. A synced image is one which already exists on the server.
     * @param filename the name of the image to insert
     * @param category the classification label of the image
     * @param filepath the filepath of the full image
     * @param cachePath the filepath of the cached path
     * @param datasetName the name of the dataset in which the image belongs
     * @return true if the image has been successfully inserted or else -1 on failure.
     */
    public boolean insertSyncedImage(String filename, String category, String filepath, String cachePath, String datasetName) {
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_NAME, filename);
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_FILE_PATH, filepath);
        contentValues.put(DatabaseHelper.IMAGE_COL_CACHE_PATH, cachePath);
        contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, datasetName);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, SYNCED);
        contentValues.put(DatabaseHelper.IMAGE_COL_USER_PK, sessionManager.getUserPK());

        long result = database.insert(DatabaseHelper.TABLE_IMAGE, null, contentValues);
        if (result == -1) Log.e(TAG, "Failed to insert Synced Image: " + filename);
        return result > 0;
    }

    /***
     * Insert a local image to the database. A local image is one which doesn't exist on the server (requires syncing).
     * @param filename the name of the image to insert
     * @param category the classification label of the image
     * @param filepath the filepath of the full image
     * @param cachePath the filepath of the cached path
     * @param datasetName the name of the dataset in which the image belongs
     */
    public void insertUnsyncedImage(String filename, String category, String filepath, String cachePath, String datasetName) {
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_NAME, filename);
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_FILE_PATH, filepath);
        contentValues.put(DatabaseHelper.IMAGE_COL_CACHE_PATH, cachePath);
        contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, datasetName);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, CREATE);
        contentValues.put(DatabaseHelper.IMAGE_COL_USER_PK, sessionManager.getUserPK());

        long result = database.insert(DatabaseHelper.TABLE_IMAGE, null, contentValues);
        if (result == -1) Log.e(TAG, "Failed to insert local Image: " + filename);
    }

    /***
     * Adds an image path to an existing image entry in the local database
     * @param filename the name of the image to update
     * @param path the filepath of the image
     * @param isCache whether the path is for a cache file or not.
     */
    public void insertImagePath(String filename, String path, boolean isCache) {
        if (path == null) return;

        open();
        ContentValues contentValues = new ContentValues();
        if (isCache) contentValues.put(DatabaseHelper.IMAGE_COL_CACHE_PATH, path);
        else contentValues.put(DatabaseHelper.IMAGE_COL_FILE_PATH, path);

        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues, DatabaseHelper.IMAGE_COL_NAME + " = ? AND " +
                DatabaseHelper.IMAGE_COL_USER_PK + " = ?", new String[]{filename, Integer.toString(sessionManager.getUserPK())});

        if (result == -1) Log.e(TAG, "Failed to update");
    }

    /***
     * Get the filepath of the cache for an image
     * @param filename the name of the image
     * @param cache whether to get the cache file path or
     * @return the filepath of the cached image or null if no path found.
     */
    public String getImagePath(String filename, boolean cache) {
        open();
        String path = null;
        String column = (cache) ? DatabaseHelper.IMAGE_COL_CACHE_PATH : DatabaseHelper.IMAGE_COL_FILE_PATH;

        Cursor cursor = database.rawQuery("SELECT " + column + " FROM " + DatabaseHelper.TABLE_IMAGE +
                        " WHERE " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                new String[]{filename, Integer.toString(sessionManager.getUserPK())});

        if (cursor.moveToFirst()) path = cursor.getString(0);
        cursor.close();
        return path;
    }

    /***
     * Gets the list of all images belonging to a particular dataset which are not set for deletion
     * @param datasetName the name of the dataset to retrieve the contents of
     * @return a list of all images belonging to the dataset which are not set for deletion
     */
    public ArrayList<ClassifiedImage> getCachedImageList(String datasetName) {
        open();
        ArrayList<ClassifiedImage> imageList = new ArrayList<>();

        String[] columns = new String[]{
                DatabaseHelper.IMAGE_COL_NAME,
                DatabaseHelper.IMAGE_COL_LABEL,
                DatabaseHelper.IMAGE_COL_FILE_PATH,
                DatabaseHelper.IMAGE_COL_CACHE_PATH,
        };

        Cursor cursor = database.query(DatabaseHelper.TABLE_IMAGE, columns, DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_SYNC_OPS + " NOT LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                new String[]{datasetName, Integer.toString(DELETE), Integer.toString(sessionManager.getUserPK())}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ClassifiedImage image = new ClassifiedImage(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                imageList.add(image);
            }
            while (cursor.moveToNext());
        }

        cursor.close();
        return imageList;
    }


    /***
     * Gets the primary key of a dataset
     * @param datasetName the name of the dataset
     * @return the primary key of the dataset or -2 if no primary key found
     */
    public int getDatasetPK(String datasetName) {
        open();
        int result;

        Cursor cursor = database.rawQuery("SELECT " + DatabaseHelper.DS_COL_PK + " FROM " + DatabaseHelper.TABLE_DATASET + " WHERE " + DatabaseHelper.DS_COL_NAME + " LIKE ? AND " +
                DatabaseHelper.DS_COL_USER_PK + " = ?", new String[]{datasetName, Integer.toString(sessionManager.getUserPK())});

        if (cursor.moveToFirst()) {
            result = cursor.getInt(0);
        } else {
            //no dataset key associated with the given dataset name
            result = -2;
        }

        cursor.close();
        return result;
    }

    /***
     * Gets the ImageDataset object which encapsulates the information of a dataset
     * @param datasetName the name of the dataset
     * @return the ImageDataset object which encapsulates the information of a dataset or null if dataset not found.
     */
    public ImageDataset getDataset(String datasetName) {
        open();
        ImageDataset dataset = null;

        String[] columns = new String[]{
                DatabaseHelper.DS_COL_PK, DatabaseHelper.DS_COL_NAME, DatabaseHelper.DS_COL_DESCRIPTION, DatabaseHelper.DS_COL_PROJECT,
                DatabaseHelper.DS_COL_LICENSE, DatabaseHelper.DS_COL_PUBLIC, DatabaseHelper.DS_COL_TAGS, DatabaseHelper.DS_COL_SYNC_OPS};

        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, columns, DatabaseHelper.DS_COL_NAME + " LIKE ? AND " + DatabaseHelper.DS_COL_USER_PK + " = ?",
                new String[]{datasetName, Integer.toString(sessionManager.getUserPK())}, null, null, null);

        if (cursor.moveToFirst()) {
            dataset = new ImageDataset(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                    cursor.getInt(4), cursor.getInt(5) == 1, cursor.getString(6), cursor.getInt(7));
        }

        cursor.close();
        return dataset;
    }


    /***
     * Sets the sync status of an image to DELETE if the image belongs to a synced dataset (a dataset which exists on the server). Else, if the image belongs
     * to a local unsynced dataset, then delete the image entry from the local database.
     * @param datasetName the name of the dataset in which the image belongs
     * @param filename the name of the image to delete
     */
    public void deleteImage(String datasetName, String filename) {
        open();
        int datasetKey = getDatasetPK(datasetName);

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, DELETE);

        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + datasetKey + " NOT IN (?,?) AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_SYNC_OPS + " NOT LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                new String[]{datasetName, Integer.toString(DEFAULT_DATASET_PK), Integer.toString(-2), filename, Integer.toString(CREATE), Integer.toString(sessionManager.getUserPK())});

        if (result == 0) {
            result = database.delete(DatabaseHelper.TABLE_IMAGE,
                    DatabaseHelper.IMAGE_COL_DATASET_NAME + " = ?  AND (" + datasetKey + " LIKE ?  OR " + DatabaseHelper.IMAGE_COL_SYNC_OPS + " LIKE ?) AND "
                            + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                    new String[]{datasetName, Integer.toString(DEFAULT_DATASET_PK), Integer.toString(CREATE), filename, Integer.toString(sessionManager.getUserPK())});
        }

        if (result == 0) Log.e(TAG, "Failed to delete image named: " + filename);
    }


    /***
     * Delete an image if and only if it has a sync status of SYNCED
     * @param datasetName the name of the dataset in which the image belongs
     * @param filename the name of the image to delete
     * @return true if the image has been deleted or false if deletion failed.
     */
    public boolean deleteSyncedImage(String datasetName, String filename) {
        open();

        long result = database.delete(DatabaseHelper.TABLE_IMAGE, DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_SYNC_OPS + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ? ",
                new String[]{datasetName, filename, Integer.toString(SYNCED), Integer.toString(sessionManager.getUserPK())});

        return result > 0;
    }


    /***
     * Updates the classification label of an image
     * @param datasetName the name of the dataset in which the image belongs
     * @param filename the name of the image
     * @param label the new classification label of the image
     */
    public void reclassifyImage(String datasetName, String filename, String label) {
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, label);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, UPDATE);

        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_SYNC_OPS + " NOT IN (?,?) AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                new String[]{datasetName, filename, Integer.toString(CREATE), Integer.toString(DELETE), Integer.toString(sessionManager.getUserPK())});

        if (result == 0) {
            contentValues.remove(DatabaseHelper.IMAGE_COL_SYNC_OPS);

            result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                    DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                    new String[]{datasetName, filename, Integer.toString(sessionManager.getUserPK())});
        }

        if (result == 0) Log.e(TAG, "Failed to reclassify image named: " + filename);
    }

    /***
     * Gets the list of all classified image objects (which encapsulate the information of an image) for unsycned images only.
     * @return list of all classified image objects for usynced images.
     */
    public ArrayList<ClassifiedImage> getUnsycnedImages() {
        open();
        ArrayList<ClassifiedImage> imageList = new ArrayList<>();
        ClassifiedImage image;

        String[] columns = new String[]{
                DatabaseHelper.IMAGE_COL_DATASET_NAME,
                DatabaseHelper.IMAGE_COL_NAME, DatabaseHelper.IMAGE_COL_LABEL, DatabaseHelper.IMAGE_COL_FILE_PATH, DatabaseHelper.IMAGE_COL_SYNC_OPS};

        Cursor cursor = database.query(DatabaseHelper.TABLE_IMAGE, columns, DatabaseHelper.IMAGE_COL_SYNC_OPS + " != ? AND " +
                        DatabaseHelper.IMAGE_COL_USER_PK + " = ?", new String[]{Integer.toString(SYNCED), Integer.toString(sessionManager.getUserPK())},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                image = new ClassifiedImage(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                        cursor.getInt(4));

                imageList.add(image);
            }
            while (cursor.moveToNext());
        }

        cursor.close();
        return imageList;
    }

    /***
     * Set the sync status of an image to SYNCED
     * @param datasetName the name of the dataset in which the image belongs
     * @param filename the name of the image to set the sync status of
     */
    public void setImageSynced(String datasetName, String filename) {
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, SYNCED);

        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ?  AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_USER_PK + " = ?", new String[]{datasetName, filename, Integer.toString(sessionManager.getUserPK())});

        if (result == 0)
            Log.e(TAG, "Failed to set the sync status of image named" + filename + " to synced");
    }


    /***
     * get the next n (Page limit) images in a dataset after a particular index
     * @param dataset the name of the dataset to load the images from
     * @param offset the number of images to skip
     * @return a batch of images in a dataset
     */
    public ArrayList<ClassifiedImage> loadImages(String dataset, int limit, int offset) {
        open();
        ArrayList<ClassifiedImage> imageList = new ArrayList<>();

        String[] columns = new String[]{
                DatabaseHelper.IMAGE_COL_NAME,
                DatabaseHelper.IMAGE_COL_LABEL,
                DatabaseHelper.IMAGE_COL_FILE_PATH,
                DatabaseHelper.IMAGE_COL_CACHE_PATH,
        };

        Cursor cursor = database.query(DatabaseHelper.TABLE_IMAGE, columns, DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_SYNC_OPS + " NOT LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?",
                new String[]{dataset, Integer.toString(DELETE), Integer.toString(sessionManager.getUserPK())}, null, null, null, offset + "," + limit);

        if (cursor.moveToFirst()) {
            do {
                ClassifiedImage image = new ClassifiedImage(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                imageList.add(image);
            }
            while (cursor.moveToNext());
        }

        cursor.close();
        return imageList;
    }

    /***
     * Gets the count of all images in a dataset regardless of the sync status
     * @param dataset the dataset in which the images belong
     * @return number of images in the dataset
     */
    public long getImageCount(String dataset) {
        open();
        return DatabaseUtils.queryNumEntries(database, DatabaseHelper.TABLE_IMAGE,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ?", new String[]{dataset, Integer.toString(sessionManager.getUserPK())});
    }

    /***
     * Gets the count of the synced images in a particular dataset
     * @param dataset the dataset in which the images belong
     * @return the number of synced images in the dataset
     */
    public long getSyncedImageCount(String dataset) {
        open();
        return DatabaseUtils.queryNumEntries(database, DatabaseHelper.TABLE_IMAGE,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_USER_PK + " = ? AND " +
                DatabaseHelper.IMAGE_COL_SYNC_OPS + " != ?", new String[]{dataset, Integer.toString(sessionManager.getUserPK()), Integer.toString(CREATE)});
    }
}
