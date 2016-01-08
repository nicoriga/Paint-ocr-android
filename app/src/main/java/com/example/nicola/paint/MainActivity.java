package com.example.nicola.paint;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Menu;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity implements View.OnClickListener {

    private DBAdapter dbAdapter;

    //custom drawing view
    private DrawingView drawView;
    //buttons
    private ImageButton currPaint, drawBtn, eraseBtn, newBtn, saveBtn;
    //sizes
    private float smallBrush, mediumBrush, largeBrush;

    private ListView lettersList;

    /**
     * The neural network.
     */
    private KohonenNetwork net;

    /**
     * The background thread used for training.
     */
    private Thread trainThread = null;

    /**
     * The downsample width for the application.
     */
    static final int DOWNSAMPLE_WIDTH = 5;

    /**
     * The down sample height for the application.
     */
    static final int DOWNSAMPLE_HEIGHT = 7;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbAdapter = new DBAdapter(this);
        context = getBaseContext();

        //get drawing view
        drawView = (DrawingView)findViewById(R.id.drawing);

        //sizes from dimensions
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        //draw button
        drawBtn = (ImageButton)findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);

        //set initial size
        drawView.setBrushSize(mediumBrush);

        //erase button
        eraseBtn = (ImageButton)findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        //new button
        newBtn = (ImageButton)findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        //save button
        saveBtn = (ImageButton)findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        lettersList = (ListView) findViewById(R.id.listview);

        lettersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Sample s = new Sample(DOWNSAMPLE_WIDTH, DOWNSAMPLE_WIDTH);
                s.setData((SampleData) lettersList.getItemAtPosition(position));
                ((ImageView) findViewById(R.id.sample_image)).setImageBitmap(s.paint(20));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //user clicked paint
    public void paintClicked(View view){
        //use chosen color

        //set erase false
        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());

        if(view!=currPaint){
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            //update ui
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;
        }
    }

    @Override
    public void onClick(View view){

        if(view.getId()==R.id.draw_btn){
            //draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            //listen for clicks on size buttons
            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(false);
                    drawView.setBrushSize(smallBrush);
                    drawView.setLastBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(false);
                    drawView.setBrushSize(mediumBrush);
                    drawView.setLastBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(false);
                    drawView.setBrushSize(largeBrush);
                    drawView.setLastBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            //show and wait for user interaction
            brushDialog.show();
        }
        else if(view.getId()==R.id.erase_btn){
            //switch to erase - choose size
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            //size buttons
            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            brushDialog.show();
        }
        else if(view.getId()==R.id.new_btn){
            //new button
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    drawView.startNew();
                    dialog.dismiss();
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            newDialog.show();
        }
        else if(view.getId()==R.id.save_btn){
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("Save drawing");
            saveDialog.setMessage("Save drawing to device Gallery?");
            saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    //save drawing
                    drawView.setDrawingCacheEnabled(true);
                    //attempt to save
                    String imgSaved = MediaStore.Images.Media.insertImage(
                            getContentResolver(), drawView.getDrawingCache(),
                            UUID.randomUUID().toString()+".png", "drawing");
                    //feedback
                    if(imgSaved!=null){
                        Toast savedToast = Toast.makeText(getApplicationContext(),
                                "Drawing saved to Gallery!", Toast.LENGTH_SHORT);
                        savedToast.show();
                    }
                    else{
                        Toast unsavedToast = Toast.makeText(getApplicationContext(),
                                "Oops! Image could not be saved.", Toast.LENGTH_SHORT);
                        unsavedToast.show();
                    }
                    drawView.destroyDrawingCache();
                }
            });
            saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int which){
                    dialog.cancel();
                }
            });
            saveDialog.show();
        }
    }

    // carica il training set e lo visualizza sulla lista a sinistra
    public void onClickLoad(View view){

        try {
            dbAdapter.open();
            ArrayList<SampleData> sd_array = new ArrayList<>();
            ArrayAdapter<SampleData> adapter;

            String line = "";
            Cursor cursor = dbAdapter.getAllRecord();
            cursor.moveToFirst();
            while ( !cursor.isAfterLast() ) {
                SampleData ds = new SampleData(cursor.getString(cursor.getColumnIndex(DBOpenHelper.CHARACTER)).charAt(0),DOWNSAMPLE_WIDTH,DOWNSAMPLE_HEIGHT);
                line = cursor.getString( cursor.getColumnIndex(DBOpenHelper.DATA));
                sd_array.add(ds);
                int idx=0;
                for ( int y=0;y<ds.getHeight();y++ ) {
                    for (int x=0;x<ds.getWidth();x++ ) {
                        ds.setData(x, y, line.charAt(idx++)=='1');
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
            dbAdapter.close();

            adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, android.R.id.text1, sd_array);
            // Assign adapter to ListView
            lettersList.setAdapter(adapter);

            Toast.makeText(getApplicationContext(),"Caricato dal file 'sample.dat'.", Toast.LENGTH_SHORT).show();
        } catch ( Exception e ) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStartTraining(View view){
        final boolean[] trained = new boolean[1];
        if ( trainThread==null ) {
            trainThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        int inputNeuron = DOWNSAMPLE_HEIGHT * DOWNSAMPLE_WIDTH;
                        int outputNeuron = lettersList.getCount();

                        TrainingSet set = new TrainingSet(inputNeuron,outputNeuron);
                        set.setTrainingSetCount(lettersList.getCount());

                        for ( int t=0;t< lettersList.getCount();t++ ) {
                            int idx=0;
                            SampleData ds = (SampleData) lettersList.getItemAtPosition(t);
                            for ( int y=0;y<ds.getHeight();y++ ) {
                                for ( int x=0;x<ds.getWidth();x++ ) {
                                    set.setInput(t,idx++,ds.getData(x,y)?.5:-.5);
                                }
                            }
                        }

                        net = new KohonenNetwork(inputNeuron,outputNeuron);
                        net.setTrainingSet(set);
                        net.learn();
                        trained[0] = true;
                        //

                    } catch ( Exception e ) {
                        trained[0] = false;
                        e.printStackTrace();
                    }
                }

            });
            trainThread.start();

            try {
                trainThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(trained[0])
                Toast.makeText(getApplicationContext(), "Training terminato!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), "Errore: Training", Toast.LENGTH_SHORT).show();

        } else {
            net.halt=true;
        }
    }

    public void onClickCancel(View view){
        int i = lettersList.getSelectedItemPosition();
        //TODO fare in modo che quando si clicchi su una lettera questa possa poi essere eliminata
        if ( i==-1 ) {
            Toast.makeText(getApplicationContext(), "eleziona una lettera da cancellare.", Toast.LENGTH_SHORT).show();
            return;
        }

        lettersList.removeViewAt(i);
    }

    public void onClickSave(View view){
    /*
        try {
            // TODO da sistemare il salvataggio... non possibile nella cartella assets

            OutputStream os;// the actual file stream
            PrintStream ps;// used to read the file line by line

            //os = new FileOutputStream( "./sample.dat",false );
            os = getResources().getAssets().
            ps = new PrintStream(os);

            for ( int i=0; i<lettersList.getCount(); i++ ) {
                SampleData ds = (SampleData)lettersList.getItemAtPosition(i);
                ps.print( ds.getLetter() + ":" );
                for ( int y=0;y<ds.getHeight();y++ ) {
                    for ( int x=0;x<ds.getWidth();x++ ) {
                        ps.print( ds.getData(x,y)?"1":"0" );
                    }
                }
                ps.println("");
            }

            ps.close();
            os.close();
            // TODo clear_actionPerformed(null);
            Toast.makeText(getApplicationContext(),"Salvato nel file 'sample.dat'.", Toast.LENGTH_SHORT).show();

        } catch ( Exception e ) {
            Toast.makeText(getApplicationContext(),"Errore Salvataggio:" + e.toString(), Toast.LENGTH_SHORT).show();

        }
        */
    }
}
