package com.example.nicola.paint;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity{

    private DBAdapter dbAdapter;

    private ArrayList<SampleData> sd_array = new ArrayList<>();
    private String newCharacter;

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

    static final int PIXEL_SIZE = 60;

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

        //set initial size
        drawView.setBrushSize(smallBrush);

        lettersList = (ListView) findViewById(R.id.listview);

        lettersList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Sample s = new Sample(DOWNSAMPLE_WIDTH, DOWNSAMPLE_WIDTH);
                s.setData((SampleData) lettersList.getItemAtPosition(position));
                ((ImageView) findViewById(R.id.sample_image)).setImageBitmap(s.paint(PIXEL_SIZE));
                ((TextView) findViewById(R.id.recognized_result)).setText("");
            }
        });
        lettersList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                final int position = pos;
                //Toast.makeText(getApplicationContext(),  "pos: " + pos, Toast.LENGTH_SHORT).show();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Eliminazione")
                        .setMessage("Eliminare il Sample?")
                        .setIcon(null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dbAdapter.open();
                                dbAdapter.deleteRecord(sd_array.get(position).getId());
                                sd_array.remove(position);
                                ArrayAdapter<SampleData> adapter = new ArrayAdapter<SampleData>(context,android.R.layout.simple_list_item_1, android.R.id.text1, sd_array);
                                lettersList.setAdapter(adapter);
                                //onClickLoad(null);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                return; // Do nothing.
                            }
                        })
                        .show();

                return true;
            }
        });

        registerForContextMenu(lettersList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void onClickNew(View view) {
        //new button
        drawView.startNew();
    }


    // carica il training set e lo visualizza sulla lista a sinistra
    public void onClickLoad(View view){

        try {
            dbAdapter.open();
            sd_array.clear();

            String line = "";
            Cursor cursor = dbAdapter.getAllRecord();
            cursor.moveToFirst();
            while ( !cursor.isAfterLast() ) {
                SampleData ds = new SampleData(cursor.getString(cursor.getColumnIndex(DBOpenHelper.CHARACTER)).charAt(0),DOWNSAMPLE_WIDTH,DOWNSAMPLE_HEIGHT, cursor.getLong(0));
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

            ArrayAdapter<SampleData> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, android.R.id.text1, sd_array);
            // Assign adapter to ListView
            lettersList.setAdapter(adapter);
            registerForContextMenu(lettersList);

            Toast.makeText(getApplicationContext(),"Caricato Terminato.", Toast.LENGTH_SHORT).show();
        } catch ( Exception e ) {
            Toast.makeText(getApplicationContext(), "Errore Caricamento!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(), "Seleziona una lettera da cancellare.", Toast.LENGTH_SHORT).show();
            return;
        }

        lettersList.removeViewAt(i);
    }

    public void onClickAdd(View view){
        showInputDialog().show();
    }

    public void onClickPreview(View view) {
        try {
            Entry entry = new Entry(drawView.canvasBitmap);
            Sample sample = new Sample(DOWNSAMPLE_WIDTH, DOWNSAMPLE_HEIGHT);
            entry.setSample(sample);
            entry.downSample();
            ((ImageView) findViewById(R.id.sample_image)).setImageBitmap(sample.paint(PIXEL_SIZE));
            ((TextView) findViewById(R.id.recognized_result)).setText("");
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(),"Errore Anteprima:" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickRecognize(View view){
        if ( net==null ) {
            Toast.makeText(getApplicationContext(),"La rete deve essere prima addestrata!", Toast.LENGTH_SHORT).show();
            return;
        }
        Entry entry = new Entry(drawView.canvasBitmap);
        Sample sample = new Sample(DOWNSAMPLE_WIDTH, DOWNSAMPLE_HEIGHT);
        entry.setSample(sample);
        entry.downSample();
        ((ImageView) findViewById(R.id.sample_image)).setImageBitmap(sample.paint(PIXEL_SIZE));

        double input[] = new double[5*7];
        int idx=0;
        SampleData ds = sample.getData();
        for ( int y=0;y<ds.getHeight();y++ ) {
            for ( int x=0;x<ds.getWidth();x++ ) {
                input[idx++] = ds.getData(x,y)?.5:-.5;
            }
        }

        double normfac[] = new double[1];
        double synth[] = new double[1];

        int best = net.winner ( input , normfac , synth ) ;
        char map[] = mapNeurons();
        Toast.makeText(getApplicationContext(),
                "  " + map[best] + "   (Il neurone vincente è il #" + best +")", Toast.LENGTH_SHORT).show();
        ((TextView) findViewById(R.id.recognized_result)).setText("" + map[best]);
    }

    /**
     * Used to map neurons to actual letters.
     *
     * @return The current mapping between neurons and letters as an array.
     */
    char []mapNeurons()
    {
        char map[] = new char[lettersList.getCount()];
        double normfac[] = new double[1];
        double synth[] = new double[1];

        for ( int i=0;i<map.length;i++ )
            map[i]='?';
        for ( int i=0;i<lettersList.getCount();i++ ) {
            double input[] = new double[5*7];
            int idx=0;
            SampleData ds = (SampleData)lettersList.getItemAtPosition(i);
            for ( int y=0;y<ds.getHeight();y++ ) {
                for ( int x=0;x<ds.getWidth();x++ ) {
                    input[idx++] = ds.getData(x,y)?.5:-.5;
                }
            }

            int best = net.winner ( input , normfac , synth ) ;
            map[best] = ds.getLetter();
        }
        return map;
    }

    protected AlertDialog showInputDialog() {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(R.id.edittext);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //newCharacter = editText.getText().toString();

                        try {

                            String character;

                            //character = newCharacter;
                            character = editText.getText().toString();

                            dbAdapter.open();

                            // effettuo il downsampling per realizzare la stringa di 1 e 0 da inserire nel database
                            Entry entry = new Entry(drawView.canvasBitmap);
                            Sample sample = new Sample(DOWNSAMPLE_WIDTH, DOWNSAMPLE_HEIGHT);
                            entry.setSample(sample);
                            entry.downSample();
                            String data = "";

                            SampleData ds = sample.getData();
                            ds.setLetter(character.charAt(0));

                            for ( int y=0;y<ds.getHeight();y++ ) {
                                for ( int x=0;x<ds.getWidth();x++ ) {
                                    data += ( ds.getData(x,y)?"1":"0" );
                                }
                            }

                            long identifier = dbAdapter.insertRecord(character, data);
                            ds.setId(identifier);
                            sd_array.add(ds);
                            ArrayAdapter<SampleData> adapter = new ArrayAdapter<SampleData>(context,android.R.layout.simple_list_item_1, android.R.id.text1, sd_array);
                            // Assign adapter to ListView
                            lettersList.setAdapter(adapter);

                            dbAdapter.close();
                            Toast.makeText(getApplicationContext(),"Aggiunta lettera.", Toast.LENGTH_SHORT).show();

                        } catch ( Exception e ) {
                            Toast.makeText(getApplicationContext(),"Errore Salvataggio:" + e.toString(), Toast.LENGTH_SHORT).show();

                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        return alert;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==R.id.list_character) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            menu.setHeaderTitle(sd_array.get(info.position).letter);
            String[] menuItems = {"Elimina"};
            for (int i = 0; i<menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String[] menuItems = {"Elimina"};
        String menuItemName = menuItems[menuItemIndex];

        //TODO eliminare la lettera usando id

        return true;
    }
}
