package io.github.waikato_ufdl;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "UFDL.db";

    public static final String TABLE_USER = "user_table";
    public static final String USER_PK = "USER_PK";
    public static final String USER_SERVER = "USER_SERVER";
    public static final String USER_USERNAME = "USER_USERNAME";
    public static final String USER_PASSWORD = "USER_PASSWORD";

    public static final String TABLE_DATASET = "dataset_table";
    public static final String DS_COL_PK = "DATASET_PK";
    public static final String DS_COL_NAME = "DATASET_NAME";
    public static final String DS_COL_DESCRIPTION = "DATASET_DESCRIPTION";
    public static final String DS_COL_PROJECT = "DATASET_PROJECT";
    public static final String DS_COL_PUBLIC = "DATASET_IS_PUBLIC";
    public static final String DS_COL_LICENSE = "DATASET_LICENSE";
    public static final String DS_COL_TAGS = "DATASET_TAGS";
    public static final String DS_COL_SYNC_OPS = "DATASET_SYNC_OPERATIONS";
    public static final String DS_COL_USER_PK = "DATASET_USER";

    public static final String TABLE_IMAGE = "image_table";
    public static final String IMAGE_COL_NAME = "IMAGE_NAME";
    public static final String IMAGE_COL_LABEL = "IMAGE_CATEGORY";
    public static final String IMAGE_COL_FILE_PATH = "IMAGE_FILE_PATH";
    public static final String IMAGE_COL_CACHE_PATH = "CACHE_FILE_PATH";
    public static final String IMAGE_COL_DATASET_NAME = "DATASET_NAME";
    public static final String IMAGE_COL_SYNC_OPS = "IMAGE_SYNC_OPERATIONS";
    public static final String IMAGE_COL_USER_PK = "IMAGE_SYNC_USER_PK";

    public static final String TABLE_LICENSE = "license_table";
    public static final String LICENSE_COL_PK = "LICENSE_PK";
    public static final String LICENSE_COL_NAME = "LICENSE_NAME";

    public static final String TABLE_PROJECT = "project_table";
    public static final String PROJECT_COL_PK = "PROJECT_PK";
    public static final String PROJECT_COL_NAME = "PROJECT_NAME";

    private static DatabaseHelper sInstance;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    /***
     * Creates the tables for the local database
     * @param sqLiteDatabase the SQLite datbase
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " +
                TABLE_USER + " (" +
                USER_PK + " INTEGER PRIMARY KEY NOT NULL, " +
                USER_SERVER + " TEXT NOT NULL, " +
                USER_USERNAME + " TEXT NOT NULL, " +
                USER_PASSWORD + " TEXT NOT NULL)"
        );

        sqLiteDatabase.execSQL("create table " +
                TABLE_DATASET + " (" +
                DS_COL_PK + " INTEGER DEFAULT -1 NOT NULL, " +
                DS_COL_NAME + " TEXT NOT NULL, " +
                DS_COL_DESCRIPTION + " TEXT NOT NULL DEFAULT '', " +
                DS_COL_PROJECT + " INTEGER DEFAULT 1 NOT NULL, " +
                DS_COL_PUBLIC + " INTEGER DEFAULT 0 NOT NULL, " +
                DS_COL_LICENSE + " INTEGER DEFAULT 1 NOT NULL, " +
                DS_COL_TAGS + " TEXT NOT NULL DEFAULT '', " +
                DS_COL_SYNC_OPS + " INTEGER NOT NULL DEFAULT 0, " +
                DS_COL_USER_PK + " INTEGER NOT NULL, " +
                "PRIMARY KEY (" + DS_COL_NAME + " , " + DS_COL_USER_PK + "), " +
                "FOREIGN KEY (" + DS_COL_LICENSE + ") " +
                "REFERENCES " + TABLE_LICENSE + " (" + LICENSE_COL_PK + "), " +
                "FOREIGN KEY (" + DS_COL_PROJECT + ") " +
                "REFERENCES " + TABLE_PROJECT + " (" + PROJECT_COL_PK + "), " +
                "FOREIGN KEY (" + DS_COL_USER_PK + ")" +
                "REFERENCES " + TABLE_USER + " (" + USER_PK + ") ON DELETE CASCADE)"
        );

        sqLiteDatabase.execSQL("create table " +
                TABLE_IMAGE + " (" +
                IMAGE_COL_NAME + " TEXT, " +
                IMAGE_COL_LABEL + " TEXT NOT NULL, " +
                IMAGE_COL_FILE_PATH + " TEXT, " +
                IMAGE_COL_CACHE_PATH + " TEXT, " +
                IMAGE_COL_DATASET_NAME + " TEXT NOT NULL, " +
                IMAGE_COL_SYNC_OPS + " INTEGER, " +
                IMAGE_COL_USER_PK + " INTEGER, " +
                "PRIMARY KEY (" + IMAGE_COL_NAME + " , " + IMAGE_COL_DATASET_NAME + "), " +
                "FOREIGN KEY (" + IMAGE_COL_DATASET_NAME + " , " +  IMAGE_COL_USER_PK + ") " +
                "REFERENCES " + TABLE_DATASET + " (" + DS_COL_NAME + " , " + DS_COL_USER_PK + ") ON UPDATE CASCADE ON DELETE CASCADE )"
        );

        sqLiteDatabase.execSQL("create table " +
                TABLE_LICENSE + " (" +
                LICENSE_COL_PK + " INTEGER PRIMARY KEY, " +
                LICENSE_COL_NAME + " TEXT)");


        sqLiteDatabase.execSQL("create table " +
                TABLE_PROJECT + " (" +
                PROJECT_COL_PK + " INTEGER PRIMARY KEY, " +
                PROJECT_COL_NAME + " TEXT)");
    }

    /***
     * Called when the database needs to be upgraded. This method will drop tables, add tables, or do anything else it needs to upgrade to the new schema version.
     * @param sqLiteDatabase the database
     * @param oldVersion the old database version
     * @param newVersion the new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_USER);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_DATASET);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_IMAGE);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_LICENSE);
        sqLiteDatabase.execSQL("drop table if exists " + TABLE_PROJECT);
        onCreate(sqLiteDatabase);
    }
}
