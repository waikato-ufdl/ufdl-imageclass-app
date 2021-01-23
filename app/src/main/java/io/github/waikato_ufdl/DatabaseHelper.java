package io.github.waikato_ufdl;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "UFDL.db";

    public static final String TABLE_DATASET = "dataset_table";
    public static final String DS_COL_PK = "DATASET_PK";
    public static final String DS_COL_NAME = "DATASET_NAME";
    public static final String DS_COL_DESCRIPTION = "DATASET_DESCRIPTION";
    public static final String DS_COL_PROJECT = "DATASET_PROJECT";
    public static final String DS_COL_PUBLIC = "DATASET_IS_PUBLIC";
    public static final String DS_COL_LICENSE = "DATASET_LICENSE";
    public static final String DS_COL_TAGS = "DATASET_TAGS";
    public static final String DS_COL_SYNC_OPS = "DATASET_SYNC_OPERATIONS";

    public static final String TABLE_IMAGE = "image_table";
    public static final String IMAGE_COL_NAME = "IMAGE_NAME";
    public static final String IMAGE_COL_LABEL = "IMAGE_CATEGORY";
    public static final String IMAGE_COL_FILE_PATH = "IMAGE_FILE_PATH";
    public static final String IMAGE_COL_CACHE_PATH = "CACHE_FILE_PATH";
    public static final String IMAGE_COL_DATASET_NAME = "DATASET_NAME";
    public static final String IMAGE_COL_SYNC_OPS = "IMAGE_SYNC_OPERATIONS";

    public static final String TABLE_LICENSE = "license_table";
    public static final String LICENSE_COL_PK = "LICENSE_PK";
    public static final String LICENSE_COL_NAME = "LICENSE_NAME";

    public static final String TABLE_PROJECT = "project_table";
    public static final String PROJECT_COL_PK = "PROJECT_PK";
    public static final String PROJECT_COL_NAME = "PROJECT_NAME";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d("onCreate: ", "CREATING DATASET TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_DATASET + " (" +
                DS_COL_PK + " INTEGER DEFAULT -1 NOT NULL, " +
                DS_COL_NAME + " TEXT PRIMARY KEY NOT NULL, " +
                DS_COL_DESCRIPTION + " TEXT NOT NULL DEFAULT '', " +
                DS_COL_PROJECT + " INTEGER DEFAULT 1 NOT NULL, " +
                DS_COL_PUBLIC + " INTEGER DEFAULT 0 NOT NULL, " +
                DS_COL_LICENSE + " INTEGER DEFAULT 1 NOT NULL, " +
                DS_COL_TAGS + " TEXT NOT NULL DEFAULT '', " +
                DS_COL_SYNC_OPS + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (" + DS_COL_LICENSE + ") " +
                "REFERENCES " + TABLE_LICENSE + " (" + LICENSE_COL_PK + "), " +
                "FOREIGN KEY (" + DS_COL_PROJECT + ") " +
                "REFERENCES " + TABLE_PROJECT+ " (" + PROJECT_COL_PK + "))"
        );

        Log.d("onCreate: ", "CREATING IMAGE TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_IMAGE + " (" +
                IMAGE_COL_NAME + " TEXT, " +
                IMAGE_COL_LABEL + " TEXT NOT NULL, " +
                IMAGE_COL_FILE_PATH + " TEXT, " +
                IMAGE_COL_CACHE_PATH + " TEXT, " +
                IMAGE_COL_DATASET_NAME + " TEXT NOT NULL, " +
                IMAGE_COL_SYNC_OPS + " INTEGER, " +
                "PRIMARY KEY (" + IMAGE_COL_NAME + " , " + IMAGE_COL_DATASET_NAME + "), " +
                "FOREIGN KEY (" + IMAGE_COL_DATASET_NAME + ")" +
                "REFERENCES " + TABLE_DATASET + " (" + DS_COL_NAME + ") ON DELETE CASCADE)"
        );
        Log.d("onCreate: ", "CREATING LICENSE TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_LICENSE + " (" +
                LICENSE_COL_PK + " INTEGER PRIMARY KEY, " +
                LICENSE_COL_NAME + " TEXT)");

        Log.d("onCreate: ", "CREATING PROJECT TABLE");
        sqLiteDatabase.execSQL("create table " +
                TABLE_PROJECT + " (" +
                PROJECT_COL_PK + " INTEGER PRIMARY KEY, " +
                PROJECT_COL_NAME + " TEXT)");
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
