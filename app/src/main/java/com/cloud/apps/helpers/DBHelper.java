package com.cloud.apps.helpers;

import static com.cloud.apps.utils.Consts.SQLITE_DATABASE_NAME;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.cloud.apps.models.LoG;


public class DBHelper extends SQLiteOpenHelper {

    Context context;

    public DBHelper(@Nullable Context context) {
        super(context, SQLITE_DATABASE_NAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("Create Table uploadedFiles(id INTEGER Primary key AUTOINCREMENT,filePath TEXT,report TEXT,status INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop Table if exists uploadedFiles");
    }

    public void insert(String filePath, String report) {
        String clearPath = filePath.replaceAll("'", "_");
        String clearReport = report.replaceAll("'", "_");
        LoG loG = new LoG(report);
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "insert into uploadedFiles(filePath,report,status) " + "Values('" + clearPath + "','" + clearReport + "'," + loG.getStatus() + ")";
        db.execSQL(query);
        db.close();
    }

    public boolean isFileUploaded(String path) {
        String clearPath = path.replaceAll("'", "_");
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "select id from uploadedFiles where filePath='" + clearPath + "' and status=3";
        Cursor cursor = db.rawQuery(query, null);
        boolean result = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return result;
    }


}

