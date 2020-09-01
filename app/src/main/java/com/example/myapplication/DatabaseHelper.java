package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "UFDL.db";

    public static final String TABLE_DATASET = "dataset_table";
    public static final String DATASET_COL_1 = "DATASET_ID"; //Local ID
    public static final String DATASET_COL_2 = "DATASET_PK"; //API ID
    public static final String DATASET_COL_3 = "DATASET_NAME";
    public static final String DATASET_COL_4 = "DATASET_PROJECT";
    public static final String DATASET_COL_5 = "DATASET_PRIVATE";
    public static final String DATASET_COL_6 = "DATASET_LICENSE";
    public static final String DATASET_COL_7 = "DATASET_TAGS";
    public static final String DATASET_COL_8 = "DATASET_SYNC_OPERATIONS";

    public static final String TABLE_IMAGE = "image_table";
    public static final String IMAGE_COL_1 = "IMAGE_NAME";
    public static final String IMAGE_COL_2 = "IMAGE_CATEGORY";
    public static final String IMAGE_COL_3 = "IMAGE_FULL_PATH";
    public static final String IMAGE_COL_4 = "IMAGE_THUMBNAIL_PATH";
    public static final String IMAGE_COL_5 = "DATASET_ID";
    public static final String IMAGE_COL_6 = "IMAGE_SYNC_OPERATIONS";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " +
                TABLE_DATASET + " (" +
                DATASET_COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DATASET_COL_2 + " INTEGER, " +
                DATASET_COL_3 + " TEXT, " +
                DATASET_COL_4 + " TEXT, " +
                DATASET_COL_5 + " INTEGER DEFAULT 1 NOT NULL, " +
                DATASET_COL_6 + " TEXT, " +
                DATASET_COL_7 + " TEXT, " +
                DATASET_COL_8 + " TEXT)");

        sqLiteDatabase.execSQL("create table " +
                TABLE_IMAGE + " (" +
                IMAGE_COL_1 + " TEXT PRIMARY KEY, " +
                IMAGE_COL_2 + " TEXT, " +
                IMAGE_COL_3 + " TEXT, " +
                IMAGE_COL_4 + " TEXT, " +
                IMAGE_COL_5 + " INTEGER, " +
                IMAGE_COL_6 + " TEXT, " +
                        "FOREIGN KEY (" + IMAGE_COL_5 + ")" +
                        "REFERENCES " + TABLE_DATASET + " (" + DATASET_COL_1 + "))"
                );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_DATASET);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_IMAGE);
        onCreate(sqLiteDatabase);
    }

    public Cursor getDatasets() {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor result = sqLiteDatabase.rawQuery("select * from " + TABLE_DATASET, null);
        return result;
    }

    public boolean createDataset(String name, String project, Boolean private_dataset, String license, String tags) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATASET_COL_3, name);
        contentValues.put(DATASET_COL_4, project);
        contentValues.put(DATASET_COL_5, private_dataset);
        contentValues.put(DATASET_COL_6, license);
        contentValues.put(DATASET_COL_7, tags);
        contentValues.put(DATASET_COL_8, "create");
        long result = sqLiteDatabase.insert(TABLE_DATASET, null, contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }

    public boolean deleteDataset(String name) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATASET_COL_8, "delete");
        sqLiteDatabase.update(TABLE_DATASET, contentValues, DATASET_COL_3 + " = ?", new String[] {name});
        return true;
    }

    public Cursor getImages(Integer dataset_id) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        Cursor result = sqLiteDatabase.rawQuery("select * from " + TABLE_IMAGE + " where " + DATASET_COL_1 + " = ?", new String[] {dataset_id.toString()});
        return result;
    }

    public boolean deleteImage(String name) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATASET_COL_8, "delete");
        sqLiteDatabase.update(TABLE_IMAGE, contentValues, IMAGE_COL_6 + " = ?", new String[] {name});
        return true;
    }

//    public boolean deleteImageLocal(String name) {
//        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
//        return sqLiteDatabase.delete(TABLE_IMAGE, )
//    }
}
