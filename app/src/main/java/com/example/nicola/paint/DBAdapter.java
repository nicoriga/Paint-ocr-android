package com.example.nicola.paint;

/**
 * Created by Nicola on 07/01/2016.
 */

    import android.content.ContentValues;
    import android.content.Context;
    import android.database.Cursor;
    import android.database.SQLException;
    import android.database.sqlite.SQLiteDatabase;

public class DBAdapter {

    private Context context;
    private SQLiteDatabase database;
    private DBOpenHelper dbHelper;

    public DBAdapter(Context context)
    {
        this.context = context;
    }

    public DBAdapter open() throws SQLException
    {
        dbHelper = new DBOpenHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close()
    {
        database.close();
        dbHelper.close();
    }

    private ContentValues createContentValues(String character, String data )
    {
        ContentValues values = new ContentValues();
        values.put( DBOpenHelper.CHARACTER, character );
        values.put( DBOpenHelper.DATA, data );

        return values;
    }


    // inserisci nuovo record
    public long insertRecord(String character, String data )
    {
        ContentValues values = createContentValues(character, data);
        return database.insertOrThrow(DBOpenHelper.TABLE, null, values);
    }


    // elimina record
    public boolean deleteRecord(long sessionID)
    {
        return database.delete(DBOpenHelper.TABLE, DBOpenHelper.ID + "=" + sessionID, null) > 0;
    }

    // preleva tutti i record
    public Cursor getAllRecord()
    {
        return database.query(DBOpenHelper.TABLE, new String[] { DBOpenHelper.ID, DBOpenHelper.CHARACTER, DBOpenHelper.DATA}, null, null, null, null, null);
    }


}
