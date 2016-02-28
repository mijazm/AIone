package com.mijaz.project.aione;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.net.*;

public class NetBot extends Activity implements AdapterView.OnItemSelectedListener {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    HttpURLConnection c;
    char command;

    Thread workerThread;

    URL url = null;

    boolean bready=false;
    Boolean stopWorker = false;

    boolean website_connected = false;


    protected PowerManager.WakeLock mWakeLock;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_bot);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        final ImageButton connect_button = (ImageButton)findViewById(R.id.connect_button);
        connect_button.setBackgroundResource(R.drawable.connect_button);
        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bready) {
                    open_bluetooth();
                    connect_button.setBackgroundResource(R.drawable.disconnect_button);
                } else if (bready) {
                    close_bluetooth();
                    connect_button.setBackgroundResource(R.drawable.connect_button);
                }
            }
        });

        final EditText url_text = (EditText)findViewById(R.id.web_address);
        final ImageView connection_notification = (ImageView)findViewById(R.id.connection_notification);
        connection_notification.setImageResource(R.drawable.connection_off);
        final Button web_connect_button = (Button)findViewById(R.id.web_connect_button);

        web_connect_button.setText("CONNECT");
        web_connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!website_connected){
                    try {
                        connection_notification.setImageResource(R.drawable.connection_on);
                        stopWorker=false;
                        url = new URL(url_text.getText().toString());
                        web_connect_button.setText("DISCONNECT");

                        connectToWebsiteThread(url);
                    }
                    catch( MalformedURLException e){

                        Toast.makeText(getApplicationContext(), "Enter a valid Address", Toast.LENGTH_SHORT).show();
                    }

                }
                else{
                    stopWorker=true;
                    web_connect_button.setText("CONNECT");
                    connection_notification.setImageResource(R.drawable.connection_off);
                    c.disconnect();
                    website_connected=false;
                }
            }
        });


        check_bluetooth();

        retrievePairedDevices();

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


        String tmp = (String) parent.getItemAtPosition(position);
        mmDevice = mBluetoothAdapter.getRemoteDevice(tmp.split("\n")[1]);

        Log.i("MyActivity", "Device selection is complete");
        Toast.makeText(this, "Device Selected:"+mmDevice.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    void check_bluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this, "No adapter found", Toast.LENGTH_SHORT).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
    }

    void open_bluetooth()
    {
        Log.i("MyActivity", "opening bluetooth connection....");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {//Standard SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            Log.i("MyActivity", "found device:"+mmDevice.getName());
        }catch (IOException e) { }
        catch (NullPointerException e){}

        try {
            if(mmSocket != null)
                mmSocket.connect();
        }catch (IOException e) {}

        try {
            if(mmSocket != null) {
                mmOutputStream = mmSocket.getOutputStream();
                mmInputStream = mmSocket.getInputStream();

            }
            else{
                Toast.makeText(this, "Please Turn on Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }catch (IOException e) { }
        bready = true;

    }

    protected void retrievePairedDevices()
    {
        Log.i("MyActivity", "I will now list the paired devices");
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {

            //Fill the spinner on view with devices
            Spinner spinner = (Spinner) findViewById(R.id.spinner);
            // Create an empty ArrayAdapter
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter <CharSequence> (this, android.R.layout.simple_spinner_item );
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            //Populate IT !!
            for (BluetoothDevice device : pairedDevices) {
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
            spinner.setOnItemSelectedListener(this);
        }
    }

    void sendMessage(String msg)
    {
        try {
            mmOutputStream.write(msg.getBytes());
        } catch (IOException e){}
    }

    void close_bluetooth()
    {
        try {
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();

        }catch(IOException e){}
        catch(NullPointerException e){}
        bready = false;
    }

    void connectToWebsiteThread(final URL url)
    {
        website_connected = true;

        workerThread = new Thread(new Runnable() {
          @Override
          public void run() {

              while(!Thread.currentThread().isInterrupted() && !stopWorker)
              connectAndAquireData(url);
          }
      });

        workerThread.start();
    }

    void connectAndAquireData(final URL url){
        try {
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.connect();
            InputStream in = c.getInputStream();

            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            int r;
            while((r=br.read())!=-1){
                command = (char)r;
            }
            //sendMessage(Character.toString(command));



            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView text = (TextView) findViewById(R.id.textView);
                    text.setText(Character.toString(command));
                    try {
                        br.close();
                        c.disconnect();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            if(stopWorker) {
                c.disconnect();
                website_connected = false;
            }
        }
        catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch( NullPointerException e){
            Log.i("MyActivity", "wrong address entered");


        }



    }

    /*
     * Some Overrides for stability
     */

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        Log.i("MyActivity", "App going on pause");
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        Log.i("MyActivity", "App going on resume");
    }

    @Override
    protected void onStop() {
        super.onStop();  // Always call the superclass method first
        close_bluetooth();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        //We close the socket when leaving


    }

    @Override
    protected void onStart() {
        super.onStart();  // Always call the superclass method first
        Log.i("MyActivity", "App going on start");
    }

    @Override
    protected void onRestart() {
        super.onRestart();  // Always call the superclass method first

        Log.i("MyActivity", "App going on restart");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        // Stop the Bluetooth RFCOMM services
        if (bready==true) close_bluetooth();
        // release screen being on
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }


}
