package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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

    public static final String TABLE_LICENSE = "license_table";
    public static final String LICENSE_COL_1 = "LICENSE_PK";
    public static final String LICENSE_COL_2 = "LICENSE_NAME";

    public static final String TABLE_PROJECT = "project_table";
    public static final String PROJECT_COL_1 = "PROJECT_PK";
    public static final String PROJECT_COL_2 = "PROJECT_NAME";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d("onCreate: ", "CREATING DATASET TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_DATASET + " (" +
                DATASET_COL_1 + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DATASET_COL_2 + " INTEGER, " +
                DATASET_COL_3 + " TEXT, " +
                DATASET_COL_4 + " TEXT, " +
                DATASET_COL_5 + " INTEGER DEFAULT 1 NOT NULL, " +
                DATASET_COL_6 + " TEXT, " +
                DATASET_COL_7 + " TEXT, " +
                DATASET_COL_8 + " TEXT, " +
                        "FOREIGN KEY (" + DATASET_COL_6 + ")" +
                        "REFERENCES " + TABLE_LICENSE + " (" + LICENSE_COL_1 + "))"
                );

        Log.d("onCreate: ", "CREATING IMAGE TABLE");
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

        Log.d("onCreate: ", "CREATING LICENSE TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_LICENSE + " (" +
                LICENSE_COL_1 + " INTEGER PRIMARY KEY, " +
                LICENSE_COL_2 + " TEXT)");

        Log.d("onCreate: ", "CREATING PROJECT TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_PROJECT + " (" +
                PROJECT_COL_1 + " INTEGER PRIMARY KEY, " +
                PROJECT_COL_2 + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_DATASET);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_IMAGE);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_LICENSE);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_PROJECT);
        onCreate(sqLiteDatabase);
    }
}
