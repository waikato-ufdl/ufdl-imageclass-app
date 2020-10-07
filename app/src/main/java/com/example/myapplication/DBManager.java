package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public List<String> getLicenses() {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        Cursor result = sqLiteDatabase.rawQuery("select * from " + TABLE_LICENSE, null);
//        return result;
        Log.d("getLicenses: ", "Create ArrayList");
        List<String> licenses = new ArrayList<String>();
        Log.d("getLicenses: ", "Open DB");
        open();
        Log.d("getLicenses: ", "Select Query");
        Cursor cursor = database.query(dbHelper.TABLE_LICENSE, new String[] {dbHelper.LICENSE_COL_2}, null, null, null, null, null, null);
//        Cursor cursor = database.rawQuery("select * from " + dbHelper.TABLE_LICENSE, null);
        Log.d("getLicenses: ", "Go through cursor list");
        if (cursor.moveToFirst()) {
            do {
                licenses.add(cursor.getString(0));
                Log.d("getLicenses: ", cursor.getString(0));
            }while (cursor.moveToNext());
        }
//        close();
        Log.d("getLicenses: ", licenses.toString());
        return licenses;
    }

    public int getLicenseKey(String licenseName) {
        open();
        int result;
        Log.d("getLicenseKey: ", "RUN CURSOR");
        Cursor cursor = database.rawQuery("SELECT * FROM "+dbHelper.TABLE_LICENSE+" WHERE "+dbHelper.LICENSE_COL_2+" = '"+licenseName+"'", null);
        Log.d("getLicenseKey: ", "CHECK FOR RESULT");
        if (cursor.moveToFirst()) {
            Log.d("getLicenseKey: ", "CURSOR FOUND RESULT");
            result = cursor.getInt(cursor.getColumnIndex(dbHelper.LICENSE_COL_1));
        } else {
            Log.d("getLicenseKey: ", "CURSOR DIDNT FIND RESULT");
            result = 0;
        }
        Log.d("getLicenseKey: ", "RESULT IS " + result);
        cursor.close();
        return result;
    }

    public void insertLicenses(int licenseKey, String licenseName){
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(LICENSE_COL_1, licenseName);
//        long result = sqLiteDatabase.insert(TABLE_LICENSE, null, contentValues);
//        if (result == -1){
//            return false;
//        } else {
//            return true;
//        }
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(dbHelper.LICENSE_COL_1, licenseKey);
        contentValues.put(dbHelper.LICENSE_COL_2, licenseName);
        database.insert(dbHelper.TABLE_LICENSE, null, contentValues);
        Log.d("insertLicenses: ", licenseKey + " " + licenseName + " INSERTED");
//        close();
    }

    public String getLicenseName(int licenseKey) {
        open();
        String result;
        Cursor cursor = database.rawQuery("SELECT * FROM "+dbHelper.TABLE_LICENSE+" WHERE "+dbHelper.LICENSE_COL_1+" = "+licenseKey+"", null);
        Log.d("getLicenseName: ", "CHECK FOR RESULT");
        if (cursor.moveToFirst()) {
            Log.d("getLicenseName: ", "CURSOR FOUND RESULT");
            result = cursor.getString(cursor.getColumnIndex(dbHelper.LICENSE_COL_2));
        } else {
            Log.d("getLicenseName: ", "CURSOR DIDNT FIND RESULT");
            result = "License Key Incorrect";
        }
        Log.d("getLicenseName: ", "RESULT IS " + result);
        cursor.close();
        return result;
    }

    public List<String> getProjects() {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        Cursor result = sqLiteDatabase.rawQuery("select * from " + TABLE_LICENSE, null);
//        return result;
        Log.d("getProjects: ", "Create ArrayList");
        List<String> projects = new ArrayList<String>();
        Log.d("getProjects: ", "Open DB");
        open();
        Log.d("getProjects: ", "Select Query");
        Cursor cursor = database.query(dbHelper.TABLE_PROJECT, new String[] {dbHelper.PROJECT_COL_2}, null, null, null, null, null, null);
//        Cursor cursor = database.rawQuery("select * from " + dbHelper.TABLE_LICENSE, null);
        Log.d("getProjects: ", "Go through cursor list");
        if (cursor.moveToFirst()) {
            do {
                projects.add(cursor.getString(0));
                Log.d("getProjects: ", cursor.getString(0));
            }while (cursor.moveToNext());
        }
//        close();
        Log.d("getProjects: ", projects.toString());
        return projects;
    }

    public int getProjectKey(String projectName) {
        open();
        int result;
        Log.d("getProjectKey: ", "RUN CURSOR");
        Cursor cursor = database.rawQuery("SELECT * FROM "+dbHelper.TABLE_PROJECT+" WHERE "+dbHelper.PROJECT_COL_2+" = '"+projectName+"'", null);
        Log.d("getProjectKey: ", "CHECK FOR RESULT");
        if (cursor.moveToFirst()) {
            Log.d("getProjectKey: ", "CURSOR FOUND RESULT");
            result = cursor.getInt(cursor.getColumnIndex(dbHelper.PROJECT_COL_1));
        } else {
            Log.d("getProjectKey: ", "CURSOR DIDNT FIND RESULT");
            result = 1;
        }
        Log.d("getProjectKey: ", "RESULT IS " + result);
        cursor.close();
        return result;
    }

    public String getProjectName(int projectKey) {
        open();
        String result;
        Cursor cursor = database.rawQuery("SELECT * FROM "+dbHelper.TABLE_PROJECT+" WHERE "+dbHelper.PROJECT_COL_1+" = "+projectKey+"", null);
        Log.d("getProjectName: ", "CHECK FOR RESULT");
        if (cursor.moveToFirst()) {
            Log.d("getProjectName: ", "CURSOR FOUND RESULT");
            result = cursor.getString(cursor.getColumnIndex(dbHelper.PROJECT_COL_2));
        } else {
            Log.d("getProjectName: ", "CURSOR DIDNT FIND RESULT");
            result = "Project Key Incorrect";
        }
        Log.d("getProjectKey: ", "RESULT IS " + result);
        cursor.close();
        return result;
    }

    public void insertProjects(int projectKey, String projectName){
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(LICENSE_COL_1, licenseName);
//        long result = sqLiteDatabase.insert(TABLE_LICENSE, null, contentValues);
//        if (result == -1){
//            return false;
//        } else {
//            return true;
//        }
        open();
        ContentValues contentValues = new ContentValues();
        contentValues.put(dbHelper.PROJECT_COL_1, projectKey);
        contentValues.put(dbHelper.PROJECT_COL_2, projectName);
        database.insert(dbHelper.TABLE_PROJECT, null, contentValues);
        Log.d("insertLicenses: ", projectKey + " " + projectName + " INSERTED");
//        close();
    }

    public Cursor getDatasets() {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        Cursor result = sqLiteDatabase.rawQuery("select * from " + TABLE_DATASET, null);
//        return result;
        String[] columns = new String[] {
                DatabaseHelper.DATASET_COL_1, DatabaseHelper.DATASET_COL_3, DatabaseHelper.DATASET_COL_4,
                DatabaseHelper.DATASET_COL_5, DatabaseHelper.DATASET_COL_6, DatabaseHelper.DATASET_COL_7
        };
        Cursor cursor = database.query(DatabaseHelper.TABLE_DATASET, columns, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void createDataset(String name, String project, Boolean private_dataset, String license, String tags) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(DATASET_COL_3, name);
//        contentValues.put(DATASET_COL_4, project);
//        contentValues.put(DATASET_COL_5, private_dataset);
//        contentValues.put(DATASET_COL_6, license);
//        contentValues.put(DATASET_COL_7, tags);
//        contentValues.put(DATASET_COL_8, "create");
//        long result = sqLiteDatabase.insert(TABLE_DATASET, null, contentValues);
//        if(result == -1)
//            return false;
//        else
//            return true;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DATASET_COL_3, name);
        contentValues.put(DatabaseHelper.DATASET_COL_4, project);
        contentValues.put(DatabaseHelper.DATASET_COL_5, private_dataset);
        contentValues.put(DatabaseHelper.DATASET_COL_6, license);
        contentValues.put(DatabaseHelper.DATASET_COL_7, tags);
        contentValues.put(DatabaseHelper.DATASET_COL_8, "create");
        database.insert(DatabaseHelper.TABLE_DATASET, null, contentValues);
    }

    public int updateDataset(int id, String name, String project, Boolean private_dataset, String license, String tags) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(DATASET_COL_3, name);
//        contentValues.put(DATASET_COL_4, project);
//        contentValues.put(DATASET_COL_5, private_dataset);
//        contentValues.put(DATASET_COL_6, license);
//        contentValues.put(DATASET_COL_7, tags);
//        contentValues.put(DATASET_COL_8, "update");
//        sqLiteDatabase.update(TABLE_DATASET, contentValues, DATASET_COL_3 + " = ?", new String[] {name});
//        return true;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DATASET_COL_3, name);
        contentValues.put(DatabaseHelper.DATASET_COL_4, project);
        contentValues.put(DatabaseHelper.DATASET_COL_5, private_dataset);
        contentValues.put(DatabaseHelper.DATASET_COL_6, license);
        contentValues.put(DatabaseHelper.DATASET_COL_7, tags);
        contentValues.put(DatabaseHelper.DATASET_COL_8, "update");
        int i = database.update(DatabaseHelper.TABLE_DATASET, contentValues, DatabaseHelper.DATASET_COL_1 + " = " + id, null);
        return i;
    }

    public int deleteDataset(int id) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(DATASET_COL_8, "delete");
//        sqLiteDatabase.update(TABLE_DATASET, contentValues, DATASET_COL_3 + " = ?", new String[] {name});
//        return true;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.DATASET_COL_8, "delete");
        int i = database.update(DatabaseHelper.TABLE_DATASET, contentValues, DatabaseHelper.DATASET_COL_1 + " = " + id, null);
        return i;
    }

    public void insertImage(String name, String category, String full_path, String thumbnail_path, int dataset_id) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(IMAGE_COL_1, name);
//        contentValues.put(IMAGE_COL_2, category);
//        contentValues.put(IMAGE_COL_3, full_path);
//        contentValues.put(IMAGE_COL_4, thumbnail_path);
//        contentValues.put(IMAGE_COL_5, dataset_id);
//        contentValues.put(IMAGE_COL_6, "add");
//        long result = sqLiteDatabase.insert(TABLE_IMAGE, null, contentValues);
//        if(result == -1)
//            return false;
//        else
//            return true;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_1, name);
        contentValues.put(DatabaseHelper.IMAGE_COL_2, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_3, full_path);
        contentValues.put(DatabaseHelper.IMAGE_COL_4, thumbnail_path);
        contentValues.put(DatabaseHelper.IMAGE_COL_5, dataset_id);
        contentValues.put(DatabaseHelper.IMAGE_COL_6, "add");
        database.insert(DatabaseHelper.TABLE_IMAGE, null, contentValues);
    }

    public int updateImage(String name, String category, int dataset_id) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(IMAGE_COL_1, name);
//        contentValues.put(IMAGE_COL_2, category);
//        contentValues.put(IMAGE_COL_5, dataset_id);
//        contentValues.put(IMAGE_COL_6, "update");
//        sqLiteDatabase.update(TABLE_IMAGE, contentValues, IMAGE_COL_1 + " = ?", new String[] {name});
//        return true;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_1, name);
        contentValues.put(DatabaseHelper.IMAGE_COL_2, category);
        contentValues.put(DatabaseHelper.IMAGE_COL_5, dataset_id);
        contentValues.put(DatabaseHelper.IMAGE_COL_6, "update");
        int i = database.update(DatabaseHelper.TABLE_IMAGE, contentValues, DatabaseHelper.DATASET_COL_1 + " = " + name, null);
        return i;
    }

    public Cursor getImages(Integer dataset_id) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        Cursor result = sqLiteDatabase.rawQuery("select * from " + TABLE_IMAGE + " where " + DATASET_COL_1 + " = ?", new String[] {dataset_id.toString()});
//        return result;
        String[] columns = new String[] {
                DatabaseHelper.IMAGE_COL_1,
                DatabaseHelper.IMAGE_COL_2,
                DatabaseHelper.IMAGE_COL_3,
                DatabaseHelper.IMAGE_COL_4
        };
        Cursor cursor = database.query(DatabaseHelper.TABLE_IMAGE, columns, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public int deleteImage(String name) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(DATASET_COL_8, "delete");
//        sqLiteDatabase.update(TABLE_IMAGE, contentValues, IMAGE_COL_6 + " = ?", new String[] {name});
//        return true;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.IMAGE_COL_6, "delete");
        int i = database.update(DatabaseHelper.TABLE_IMAGE, contentValues, DatabaseHelper.IMAGE_COL_1 + " = " + name, null);
        return i;
    }

//    public boolean deleteImageLocal(String name) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        return sqLiteDatabase.delete(TABLE_IMAGE, )
//    }
}
