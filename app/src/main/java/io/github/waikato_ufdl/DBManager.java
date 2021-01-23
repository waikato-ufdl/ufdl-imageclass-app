package io.github.waikato_ufdl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.ArrayRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.waikato_ufdl.ui.images.ClassifiedImage;
import io.github.waikato_ufdl.ui.manage.ImageDataset;

public class DBManager {
    private DatabaseHelper dbHelper;
    private Context context;
    private SQLiteDatabase database;

    /**
     * Default datasetPK for new datasets
     */
    private final int DEFAULT_DATASET_PK = -1;
    private final int SYNCED = 0;
    private final int CREATE = 1;
    private final int UPDATE = 2;
    private final int DELETE = 3;

    public DBManager(Context c) {
        context = c;
    }

    /**
     * opens the database connection
     *
     * @return DBManager object
     * @throws SQLException
     */
    public synchronized DBManager open() {

        if(dbHelper == null) {
            dbHelper = new DatabaseHelper(context);
            database = dbHelper.getWritableDatabase();

            //enable foreign keys
            database.execSQL("PRAGMA foreign_keys = ON;");
        }
        return this;
    }

    /**
     * closes the database connection
     */
    public void close() {
        dbHelper.close();
    }

    /**
     * Method to retrieve licenses stored in database table
     *
     * @return list of licenses (strings)
     */
    public List<String> getLicenses() {
        Log.d("getLicenses: ", "Create ArrayList");
        List<String> licenses = new ArrayList<String>();

        //open database connection
        open();

        //query to return cursor object containing license data
        Cursor cursor = database.query(dbHelper.TABLE_LICENSE, new String[]{dbHelper.LICENSE_COL_NAME}, null, null, null, null, null, null);

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
        //close();

        Log.d("getLicenses: ", licenses.toString());
        return licenses;
    }

    /**
     * Method to retrieve the primary key of a given license name
     *
     * @param licenseName the name of the license
     * @return the primary key of a license given its name
     */
    public int getLicenseKey(String licenseName) {

        //open database connection
        open();
        int result;

        //query to get the primary key of a license given the name of the particular license (character case shouldn't affect retrieval)
        Cursor cursor = database.rawQuery("SELECT * FROM " + dbHelper.TABLE_LICENSE + " WHERE " + dbHelper.LICENSE_COL_NAME + " = '" + licenseName + "' COLLATE NOCASE", null);

        //check to see if the cursor contains any data
        if (cursor.moveToFirst()) {
            //get license key
            result = cursor.getInt(0);
        } else {
            //if no key was found return -1
            result = -1;
        }
        Log.d("getLicenseKey: ", "RESULT IS " + result);
        cursor.close();

        //close the database connection
        //close();
        return result;
    }

    public void insertLicenses(int licenseKey, String licenseName) {
        //open database connection
        open();
        ContentValues contentValues = new ContentValues();

        //put the license key and name into the content values
        contentValues.put(dbHelper.LICENSE_COL_PK, licenseKey);
        contentValues.put(dbHelper.LICENSE_COL_NAME, licenseName);

        //insert the content values into the license table
        long result = database.insert(dbHelper.TABLE_LICENSE, null, contentValues);

        if (result == -1) {
            Log.d("insertLicenses: ", licenseKey + " " + licenseName + " FAILED TO INSERT");
        } else {
            Log.d("insertLicenses: ", licenseKey + " " + licenseName + " INSERTED");
        }

        //close();
    }

    public String getLicenseName(int licenseKey) {
        //open database connection
        open();
        String result;

        //execute query to retrieve a license name given the primary key of the license
        Cursor cursor = database.rawQuery("SELECT * FROM " + dbHelper.TABLE_LICENSE + " WHERE " + dbHelper.LICENSE_COL_PK + " = " + licenseKey + "", null);

        //check to see if cursor contains any data
        if (cursor.moveToFirst()) {
            //get the license name
            result = cursor.getString(cursor.getColumnIndex(dbHelper.LICENSE_COL_NAME));
        } else {
            //no license found
            result = null;
        }
        Log.d("getLicenseName: ", "RESULT IS " + result);

        //close the cursor and database connection
        cursor.close();
        //close();
        return result;
    }

    public List<String> getProjects() {
        List<String> projects = new ArrayList<String>();

        //open database connection
        open();

        //query to get project names
        Cursor cursor = database.query(dbHelper.TABLE_PROJECT, new String[]{dbHelper.PROJECT_COL_NAME}, null, null, null, null, null, null);

        //check if cursor contains any data
        if (cursor.moveToFirst()) {
            do {
                //add project names to projects list
                projects.add(cursor.getString(0));
            }
            //while there is more project names
            while (cursor.moveToNext());
        }

        //close cursor and database connection
        cursor.close();
        //close();
        Log.d("getProjects: ", projects.toString());
        return projects;
    }


    public int getProjectKey(String projectName) {
        //open database connection
        open();
        int result;

        //query to retrieve project key given the name of the project
        Cursor cursor = database.rawQuery("SELECT * FROM " + dbHelper.TABLE_PROJECT + " WHERE " + dbHelper.PROJECT_COL_NAME + " = '" + projectName + "' COLLATE NOCASE", null);

        //check if cursor contains any data
        if (cursor.moveToFirst()) {
            //get project key
            result = cursor.getInt(cursor.getColumnIndex(dbHelper.PROJECT_COL_PK));
        } else {
            //no project key found
            result = -1;
        }
        Log.d("getProjectKey: ", "RESULT IS " + result);

        //close cursor and database connection
        cursor.close();
        //close();

        return result;
    }

    public String getProjectName(int projectKey) {
        //open database connection
        open();
        String result;

        //get the project name associated with a given project key
        Cursor cursor = database.rawQuery("SELECT * FROM " + dbHelper.TABLE_PROJECT + " WHERE " + dbHelper.PROJECT_COL_PK + " = " + projectKey + "", null);

        //check if the cursor contains data
        if (cursor.moveToFirst()) {
            //retrieve the project name
            result = cursor.getString(cursor.getColumnIndex(dbHelper.PROJECT_COL_NAME));
        } else {
            //no project name associated with the given key
            result = null;
        }
        Log.d("getProjectKey: ", "RESULT IS " + result);

        //close cursor and database connection
        cursor.close();
        //close();
        return result;
    }

    public void insertProjects(int projectKey, String projectName) {
        //open database connection
        open();
        ContentValues contentValues = new ContentValues();

        //insert project key and project name into content values
        contentValues.put(dbHelper.PROJECT_COL_PK, projectKey);
        contentValues.put(dbHelper.PROJECT_COL_NAME, projectName);

        //insert the content values into the project table
        long result = database.insert(dbHelper.TABLE_PROJECT, null, contentValues);

        //check if insertion failed or succeeded
        if (result == -1) {
            Log.d("insertProject: ", projectKey + " " + projectName + " FAILED TO INSERT");
        } else {
            Log.d("insertProject: ", projectKey + " " + projectName + " INSERTED");
        }

        //close the database connection
        //close();
    }


    public ArrayList<ImageDataset> getDatasetList() {
        //establish database connection
        open();
        ArrayList<ImageDataset> datasetList = new ArrayList<>();
        ImageDataset dataset;

        String[] columns = new String[]{
                DatabaseHelper.DS_COL_PK, DatabaseHelper.DS_COL_NAME, DatabaseHelper.DS_COL_DESCRIPTION, DatabaseHelper.DS_COL_PROJECT,
                DatabaseHelper.DS_COL_LICENSE, DatabaseHelper.DS_COL_PUBLIC, DatabaseHelper.DS_COL_TAGS, DatabaseHelper.DS_COL_SYNC_OPS};

        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, columns, DatabaseHelper.DS_COL_SYNC_OPS + " != ?" ,new String[] {Integer.toString(DELETE)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                dataset = new ImageDataset(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5) == 1, cursor.getString(6), cursor.getInt(7));

                datasetList.add(dataset);
            }
            while (cursor.moveToNext());
        }

        //close();
        return datasetList;
    }

    /**
     * New datasets are datasets which have not been added to the backend yet and hence, they do not have a dataset primary key until they are synchronised.
     */

    public void insertLocalDataset(String name, String description, int project, int license, Boolean isPublic, String tags) {
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_NAME, name);
        contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
        contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
        contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
        contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
        contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);
        contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, CREATE);

        try {
            long result = database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);

            if (result == -1) {
                Log.d("insert DB: ", name + " " + project + " " + isPublic + " " + license + " " + tags + " " + " FAILED TO INSERT");
            } else {
                Log.d("insert DB: ", name + " " + project + " " + isPublic + " " + license + " " + tags + " " + " INSERTED");
            }
        } catch (SQLException e) {
            System.out.print(e.getMessage());
        }

        //close();
    }

    public void updateLocalDataset(String oldName, String newName, String description, int project, int license, Boolean isPublic, String tags) {

        //open the database connection
        open();

        //disable foreign keys
        database.execSQL("PRAGMA foreign_keys = OFF;");

        //add all the  values to content values
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_NAME, newName);
        contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
        contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
        contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
        contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
        contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);

        try {
            //update any existing entry where the dataset name matches the old name of the dataset and where the sync_operation is set to create
            //this ensures that the dataset will be created using the updated information rather than creating a dataset using old information and then updating it afterwards
            int result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " = ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " = ? ", new String[]{oldName, Integer.toString(CREATE)});

            if(result > 0 && !oldName.equals(newName))
            {
                contentValues.clear();
                contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, newName);
                database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                        DatabaseHelper.DS_COL_NAME + " = ? " , new String[]{oldName});
            }

            Log.d("SQLite", "Number of Entries Updated: " + result);

        } catch (SQLException e) {
            Log.e("SQLite: ", e.getMessage());
        }

        //close the database connection
        //close();
    }

    public void updateLocalDatasetAfterSync(int datasetPk , String oldName, String newName, String description, int project, int license, Boolean isPublic, String tags) {

        //open the database connection
        open();

        //disable foreign keys
        database.execSQL("PRAGMA foreign_keys = OFF;");

        //add all the  values to content values
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_PK, datasetPk);
        contentValues.put(DatabaseHelper.DS_COL_NAME, newName);
        contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
        contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
        contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
        contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
        contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);

        try {
            //update any existing entry where the dataset name matches the old name of the dataset and where the sync_operation is set to create
            //this ensures that the dataset will be created using the updated information rather than creating a dataset using old information and then updating it afterwards
            int result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " = ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " = ? ", new String[]{oldName, Integer.toString(SYNCED)});

            if(result > 0 && !oldName.equals(newName))
            {
                contentValues.clear();
                contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, newName);
                database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                        DatabaseHelper.DS_COL_NAME + " = ? " , new String[]{oldName});
            }

            Log.d("SQLite", "Number of Entries Updated: " + result);

        } catch (SQLException e) {
            Log.e("SQLite: ", e.getMessage());
        }

        //close the database connection
        //close();
    }

    public void deleteLocalDataset(String datasetName) {
        open();

        try {
            //delete a particular dataset if it has a PK = -1 (local) or it has a status of synced
            int result = database.delete(DatabaseHelper.TABLE_DATASET,
                    DatabaseHelper.DS_COL_NAME + " LIKE ? AND (" + DatabaseHelper.DS_COL_PK + " LIKE ? OR "
                            + DatabaseHelper.DS_COL_SYNC_OPS + " = ?)", new String[]{datasetName, Integer.toString(DEFAULT_DATASET_PK), Integer.toString(SYNCED)});

            Log.e("SQLite", "Number of Entries Deleted: " + result);

        } catch (SQLException e) {
            Log.e("SQLite: ", e.getMessage());
        }

        //close();
    }

    /**
     * Synced datasets are ones which are already on the backend and hence, they have already have a primary key allocated to them
     **/


    public void insertSyncedDataset(int datasetPK, String name, String description, int project, int license, Boolean isPublic, String tags) {
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

        try {
            long result = database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);

            if (result == -1) {
                Log.d("insert DB: ", datasetPK + name + " " + project + " " + isPublic + " " + license + " " + tags + " " + " FAILED TO INSERT");
            } else {
                Log.d("insert DB: ", datasetPK + name + " " + project + " " + isPublic + " " + license + " " + tags + " " + " INSERTED");
            }
        } catch (SQLException e) {
            System.out.print(e.getMessage());
        }

        //close();
    }


    /**
     * CASE 1: no dataset entry with the same name --> add entry and set SYNC_OP = update
     * CASE 2: there is an existing entry with the same name and has it's SYNC_OP = create --> use updateNewDataset
     * CASE 3: there is an existing entry with the same name but has it's SYNC_OP = synced|update --> update the entry and set SYNC_OP = update
     * take into account these cases and modify this method
     */

    public void updateDataset(String oldName, String newName, String description, int project, int license, Boolean isPublic, String tags) {

        //open the database connection
        open();

        //disable foreign keys
        database.execSQL("PRAGMA foreign_keys = OFF;");

        try {
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

            //update a particular dataset given that its sync status is not set to DELETE or CREATE
            result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " LIKE ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " NOT IN (?,?)", new String[]{oldName, Integer.toString(DELETE), Integer.toString(CREATE)});

            Log.d("SQLite", "Number of Entries Updated: " + result);

            if(result > 0 && !oldName.equals(newName))
            {
                contentValues.clear();
                contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, newName);
                database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                        DatabaseHelper.DS_COL_NAME + " = ? " , new String[]{oldName});
            }

            //if the sync status of the dataset is set to create, the dataset is still local and so call updateLocalDataset
            if (result == 0 && getDatasetSyncOpStatus(oldName) == CREATE) {
                updateLocalDataset(oldName, newName, description, project, license, isPublic, tags);
            }
            //else, create a new entry using content values
            else {
                database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);
            }
        } catch (SQLException e) {
            Log.e("SQLite: ", e.getMessage());
        }

        //close the database connection
        //close();
    }


    public void updateDataset(int datasetPK, String oldName, String newName, String description, int project, int license, Boolean isPublic, String tags) {

        //open the database connection
        open();

        //disable foreign keys
        database.execSQL("PRAGMA foreign_keys = OFF;");

        try {
            //add all the  values to content values
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.DS_COL_PK, datasetPK);
            contentValues.put(DatabaseHelper.DS_COL_NAME, newName);
            contentValues.put(DatabaseHelper.DS_COL_DESCRIPTION, description);
            contentValues.put(DatabaseHelper.DS_COL_PROJECT, project);
            contentValues.put(DatabaseHelper.DS_COL_PUBLIC, isPublic);
            contentValues.put(DatabaseHelper.DS_COL_LICENSE, license);
            contentValues.put(DatabaseHelper.DS_COL_TAGS, tags);
            contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, UPDATE);

            int result;

            //update a particular dataset given that its sync status is not set to DELETE or CREATE
            result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                    DatabaseHelper.DS_COL_NAME + " LIKE ? AND " + DatabaseHelper.DS_COL_SYNC_OPS + " NOT IN (?,?)", new String[]{oldName, Integer.toString(DELETE), Integer.toString(CREATE)});

            if(result > 0 && !oldName.equals(newName))
            {
                contentValues.clear();
                contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, newName);
                database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                        DatabaseHelper.DS_COL_NAME + " = ? " , new String[]{oldName});
            }

            //if the sync status of the dataset is set to create, the dataset is still local and so call updateLocalDataset
            if (result == 0 && getDatasetSyncOpStatus(oldName) == CREATE) {
                updateLocalDataset(oldName, newName, description, project, license, isPublic, tags);
            }
            //else, create a new entry using content values
            else {
                database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);
            }
        } catch (SQLException e) {
            Log.e("SQLite: ", e.getMessage());
        }

        //close the database connection
        //close();
    }

    public void deleteSyncedDataset(String datasetName) {
        open();

        try {
            //query to check if there is already a dataset entry in the database with the particular name
            String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_DATASET + " WHERE " + DatabaseHelper.DS_COL_NAME + " LIKE ? ";
            Cursor cursor = database.rawQuery(query, new String[]{datasetName});
            int count = 0;

            //check that cursor is not null
            if (cursor.moveToFirst()) {
                //check how may rows have been returned
                count = cursor.getInt(0);
            }

            //close cursor
            cursor.close();

            ContentValues contentValues = new ContentValues();
            int result = 0;

            //if there is already a dataset with the same name, change it's SYNC_OP to delete if it's a synced dataset (has a dataset primary key != -1)
            if (count > 0) {
                contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, DELETE);
                result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                        DatabaseHelper.DS_COL_PK + " != ? AND " + DatabaseHelper.DS_COL_NAME + " LIKE ?", new String[]{Integer.toString(DEFAULT_DATASET_PK), datasetName});
                Log.e("SQLite", "Number of Entries Deleted: " + result);
            }

            //there is a dataset with the same column name, however it's PK = -1 (it's an unsynced dataset to execute deleteNewDataset command)
            if (count > 0 && result == 0) {
                deleteLocalDataset(datasetName);
            }
            //there is no dataset with the same column name, so insert an entry with sync op = delete
            else {
                contentValues.put(DatabaseHelper.DS_COL_NAME, datasetName);
                contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, DELETE);
                database.insertOrThrow(DatabaseHelper.TABLE_DATASET, null, contentValues);
            }
        } catch (SQLException e) {
            Log.e("SQLite: ", e.getMessage());
        }

        //close the database connection
        //close();
    }

    public int getDatasetSyncOpStatus(String datasetName) {
        open();
        int status = -1;

        Cursor cursor = database.query(dbHelper.TABLE_DATASET, new String[]{dbHelper.DS_COL_SYNC_OPS}, DatabaseHelper.DS_COL_NAME + " = ?", new String[]{datasetName}, null, null, null, null);

        if (cursor.moveToFirst()) {
            status = cursor.getInt(0);
        }

        return status;
    }

    public void setDatasetSynced(String datasetName) {
        //establish database connection
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DS_COL_SYNC_OPS, SYNCED);

        int result = database.update(DatabaseHelper.TABLE_DATASET, contentValues,
                DatabaseHelper.DS_COL_NAME + " LIKE ? ", new String[]{datasetName});

        Log.e("SQLite: ", "Synced " + result + " datasets");

        //close();
    }

    public ArrayList<ImageDataset> getUnsyncedDatasets() {
        //establish database connection
        open();
        ArrayList<ImageDataset> datasetList = new ArrayList<>();
        ImageDataset dataset;

        String[] columns = new String[]{
                DatabaseHelper.DS_COL_PK, DatabaseHelper.DS_COL_NAME, DatabaseHelper.DS_COL_DESCRIPTION, DatabaseHelper.DS_COL_PROJECT,
                DatabaseHelper.DS_COL_LICENSE, DatabaseHelper.DS_COL_PUBLIC, DatabaseHelper.DS_COL_TAGS, DatabaseHelper.DS_COL_SYNC_OPS};
        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, columns, DatabaseHelper.DS_COL_SYNC_OPS + " != ?", new String[]{Integer.toString(SYNCED)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                dataset = new ImageDataset(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3),
                        cursor.getInt(4), cursor.getInt(5) == 1, cursor.getString(6), cursor.getInt(7));

                datasetList.add(dataset);
            }
            while (cursor.moveToNext());
        }

        //close();
        return datasetList;
    }


    public boolean insertSyncedImage(String filename, String category, String filepath, String cachePath, String datasetName) {

        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_NAME, filename);
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_FILE_PATH, filepath);
        contentValues.put(DatabaseHelper.IMAGE_COL_CACHE_PATH, cachePath);
        contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, datasetName);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, SYNCED);


        long result = database.insert(DatabaseHelper.TABLE_IMAGE, null, contentValues);

        if (result == -1) {
            Log.e("SQLite :", "Failed to insert Image: " + filename);
        }

        //close();
        return result > 0;
    }

    public boolean insertUnsyncedImage(String filename, String category, String filepath, String cachePath, String datasetName) {

        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_NAME, filename);
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_FILE_PATH, filepath);
        contentValues.put(DatabaseHelper.IMAGE_COL_CACHE_PATH, cachePath);
        contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, datasetName);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, CREATE);


        long result = database.insert(DatabaseHelper.TABLE_IMAGE, null, contentValues);

        if (result == -1) {
            Log.e("SQLite :", "Failed to insert Image: " + filename);
        }

        return result > 0;
    }


    public void insertImageCachePath(String filename, String path)
    {
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_CACHE_PATH, path);

        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues, DatabaseHelper.IMAGE_COL_NAME + " = ?", new String[] {filename});
    }

    public String getImageCachePath(String filename)
    {
        open();

        Cursor cursor = database.rawQuery("SELECT " +  DatabaseHelper.IMAGE_COL_CACHE_PATH  + " FROM " + dbHelper.TABLE_IMAGE + " WHERE " + dbHelper.IMAGE_COL_NAME + " LIKE ?" ,  new String[] {filename});

        if(cursor.moveToFirst())
        {
            return cursor.getString(0);
        }

        return null;
    }

    public void updateImageCategory(String datasetName, String fileName, String category) {

        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_NAME, fileName);
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_DATASET_NAME, datasetName);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, UPDATE);
        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.DS_COL_NAME + " = ? AND " + DatabaseHelper.IMAGE_COL_NAME + " = ?" , new String[] {datasetName, fileName});

        if(result > 0)
        {
            Log.e("SQLite: ", "image category changed");
        }
    }


    public ArrayList<ClassifiedImage> getCachedImageList(String datasetName) {

        ArrayList<ClassifiedImage> imageList = new ArrayList<>();

        String[] columns = new String[]{
                DatabaseHelper.IMAGE_COL_NAME,
                DatabaseHelper.IMAGE_COL_LABEL,
                DatabaseHelper.IMAGE_COL_FILE_PATH,
                DatabaseHelper.IMAGE_COL_CACHE_PATH,
        };

        //get all images which belong to a particular dataset which don't have a sync status = delete
        Cursor cursor = database.query(DatabaseHelper.TABLE_IMAGE, columns, DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " +
                DatabaseHelper.IMAGE_COL_SYNC_OPS  + " NOT LIKE ?", new String[] {datasetName, Integer.toString(DELETE)}, null, null, null, null);

        if(cursor.moveToFirst())
        {
            //while there are still rows in the cursor, create classified image objects and add them to the image list
            do {
                ClassifiedImage image = new ClassifiedImage(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
                imageList.add(image);
            }

            //return image list
            while(cursor.moveToNext());
        }

        return imageList;
    }


    public int getDatasetPK(String datasetName)
    {
        open();
        int result;

        //get the primry key associated with the given dataset name
        Cursor cursor = database.rawQuery("SELECT " +  DatabaseHelper.DS_COL_PK + " FROM " + dbHelper.TABLE_DATASET + " WHERE " + dbHelper.DS_COL_NAME + " LIKE ?",
                new String[] {datasetName});

        //check if the cursor contains data
        if (cursor.moveToFirst()) {
            //retrieve the dataset key
            result = cursor.getInt(0);
        } else {
            //no dataset key associated with the given dataset name
            result = -2;
        }
        Log.d("SQLite: ", "RESULT IS " + result);

        //close cursor and database connection
        cursor.close();
        return result;
    }


    public boolean deleteImage(String datasetName, String filename) {

        open();
        int datasetKey = getDatasetPK(datasetName);

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, DELETE);

        int result  = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + datasetKey + " NOT IN (?,?) AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                DatabaseHelper.IMAGE_COL_SYNC_OPS + " NOT LIKE ?",
                new String[] {datasetName, Integer.toString(DEFAULT_DATASET_PK), Integer.toString(-2), filename, Integer.toString(CREATE)});

        if(result == 0)
        {
            result  = database.delete(DatabaseHelper.TABLE_IMAGE,
                    DatabaseHelper.IMAGE_COL_DATASET_NAME + " = ?  AND (" +
                            datasetKey + " LIKE ?  OR " + DatabaseHelper.IMAGE_COL_SYNC_OPS + " LIKE ?) AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ?",
                    new String[] {datasetName, Integer.toString(DEFAULT_DATASET_PK), Integer.toString(CREATE), filename});
        }

        return result > 0;
    }


    public void deleteSyncedImage(String datasetName, String filename)
    {
        open();

        database.delete(DatabaseHelper.TABLE_IMAGE, DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                DatabaseHelper.IMAGE_COL_SYNC_OPS + " LIKE ?", new String[] {datasetName, filename, Integer.toString(SYNCED)});

    }


    public void reclassifyImage(String datasetName, String filename, String label) {

        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_LABEL, label);
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, UPDATE);

        int result  = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ? AND " +
                        DatabaseHelper.IMAGE_COL_SYNC_OPS + " NOT IN (?,?)",
                new String[] {datasetName, filename, Integer.toString(CREATE), Integer.toString(DELETE)});

        if(result == 0)
        {
            contentValues.remove(DatabaseHelper.IMAGE_COL_SYNC_OPS);

            result  = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                    DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ? AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ?",
                    new String[] {datasetName, filename});
        }
    }

    public ArrayList<ClassifiedImage> getUnsycnedImages()
    {
        open();
        ArrayList<ClassifiedImage> imageList = new ArrayList<>();
        ClassifiedImage image;

        String[] columns = new String[]{
                DatabaseHelper.IMAGE_COL_DATASET_NAME,
                DatabaseHelper.IMAGE_COL_NAME, DatabaseHelper.IMAGE_COL_LABEL, DatabaseHelper.IMAGE_COL_FILE_PATH, DatabaseHelper.IMAGE_COL_SYNC_OPS};

        Cursor cursor = database.query(DatabaseHelper.TABLE_IMAGE, columns, DatabaseHelper.IMAGE_COL_SYNC_OPS + " != ?", new String[]{Integer.toString(SYNCED)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {

                Log.e("CHECK", cursor.getString(0) + " " + cursor.getString(1) + " " + cursor.getString(2) + " " + cursor.getString(3) + " " +
                        cursor.getInt(4));

                image = new ClassifiedImage(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                        cursor.getInt(4));

                imageList.add(image);
            }
            while (cursor.moveToNext());
        }

        cursor.close();

        //close();
        return imageList;
    }

    public void setImageSynced(String datasetName, String filename) {
        //establish database connection
        open();

        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_SYNC_OPS, SYNCED);

        int result = database.update(DatabaseHelper.TABLE_IMAGE, contentValues,
                DatabaseHelper.IMAGE_COL_DATASET_NAME + " LIKE ?  AND " + DatabaseHelper.IMAGE_COL_NAME + " LIKE ?", new String[]{datasetName, filename});

        //close();
    }

//    public boolean deleteImageLocal(String name) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        return sqLiteDatabase.delete(TABLE_IMAGE, )
//    }
}
