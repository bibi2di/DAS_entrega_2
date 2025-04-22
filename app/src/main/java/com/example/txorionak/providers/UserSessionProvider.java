package com.example.txorionak.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserSessionProvider extends ContentProvider {
    // Authority for this provider
    public static final String AUTHORITY = "com.example.txorionak.sessionprovider";

    // Base URI for accessing this provider
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sessions");

    // Database constants
    private static final String DB_NAME = "user_sessions.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_SESSIONS = "sessions";

    // Column names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_REMEMBER_ME = "remember_me";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // URI matcher codes
    private static final int SESSIONS = 1;
    private static final int SESSION_ID = 2;

    // URI matcher
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, "sessions", SESSIONS);
        uriMatcher.addURI(AUTHORITY, "sessions/#", SESSION_ID);
    }

    // Database helper
    private DatabaseHelper dbHelper;

    // Database helper class
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_SESSIONS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_EMAIL + " TEXT, " +
                    COLUMN_PASSWORD + " TEXT, " +
                    COLUMN_REMEMBER_ME + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_TIMESTAMP + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case SESSIONS:
                cursor = db.query(TABLE_SESSIONS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case SESSION_ID:
                String id = uri.getLastPathSegment();
                cursor = db.query(TABLE_SESSIONS, projection, COLUMN_ID + "=?", new String[]{id},
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case SESSIONS:
                return "vnd.android.cursor.dir/sessions";
            case SESSION_ID:
                return "vnd.android.cursor.item/session";
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != SESSIONS) {
            throw new IllegalArgumentException("Invalid URI for insert");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(TABLE_SESSIONS, null, values);

        if (id > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;

        switch (uriMatcher.match(uri)) {
            case SESSIONS:
                count = db.delete(TABLE_SESSIONS, selection, selectionArgs);
                break;
            case SESSION_ID:
                String id = uri.getLastPathSegment();
                count = db.delete(TABLE_SESSIONS, COLUMN_ID + "=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;

        switch (uriMatcher.match(uri)) {
            case SESSIONS:
                count = db.update(TABLE_SESSIONS, values, selection, selectionArgs);
                break;
            case SESSION_ID:
                String id = uri.getLastPathSegment();
                count = db.update(TABLE_SESSIONS, values, COLUMN_ID + "=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}