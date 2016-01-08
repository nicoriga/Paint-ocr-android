package com.example.nicola.paint;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Nicola on 07/01/2016.
 */
public class DBOpenHelper extends SQLiteOpenHelper{

    // The keys of the database.
    private static final String DATABASE_NAME = "Paint.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE = "training_set";
    public static final String ID = "id";
    public static final String CHARACTER = "character";
    public static final String DATA = "data";
    private Context context;

    public DBOpenHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String sql = "create table " + TABLE +
                "( "+ ID + " integer primary key autoincrement, " +
                CHARACTER + " text not null, " +
                DATA + " text not null );";
        db.execSQL(sql);
        dataToSql(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);

    }

    private String dataToSql(SQLiteDatabase db){
        String query = "";

        try {
            InputStream is;// the actual file stream
            BufferedReader r;// used to read the file line by line

            is = context.getResources().getAssets().open("sample.dat");
            r = new BufferedReader(new InputStreamReader(is));

            String line;

            while ((line = r.readLine()) != null) {
                query = "INSERT INTO "+ TABLE + " (" + CHARACTER + "," + DATA  + ") values ('" + line.charAt(0) +"' , '" + line.substring(2) + "' );";
                db.execSQL(query);
                query = "";
            }

            r.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return  query;
    }
}

