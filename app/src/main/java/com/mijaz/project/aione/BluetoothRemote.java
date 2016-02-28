package com.mijaz.project.aione;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;

import java.io.InputStream;
import java.io.OutputStream;

import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
/**
 * Created by matrixmachine on 22/2/16.
 */
public class BluetoothRemote extends Activity implements AdapterView.OnItemSelectedListener,View.OnClickListener{

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    boolean bready=false;


    protected PowerManager.WakeLock mWakeLock;


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

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_layout);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();

        final ImageButton connect_button = (ImageButton)findViewById(R.id.connect_button);
        connect_button.setBackgroundResource(R.drawable.connect_button);
        connect_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bready){
                   open_bluetooth();
                    connect_button.setBackgroundResource(R.drawable.disconnect_button);
                }
                else if(bready){
                    close_bluetooth();
                    connect_button.setBackgroundResource(R.drawable.connect_button);
                }
            }
        });


        ImageButton forward_button = (ImageButton)findViewById(R.id.forward_button);
        ImageButton backward_button = (ImageButton)findViewById(R.id.backward_button);
        ImageButton left_button = (ImageButton)findViewById(R.id.left_button);
        ImageButton right_button = (ImageButton)findViewById(R.id.right_button);
        ImageButton stop_button = (ImageButton)findViewById(R.id.stop_button);

        forward_button.setOnClickListener(this);
        backward_button.setOnClickListener(this);
        right_button.setOnClickListener(this);
        left_button.setOnClickListener(this);
        stop_button.setOnClickListener(this);

        check_bluetooth();

        retrievePairedDevices();


    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.stop_button:
                if(bready==true)
                    sendMessage(new String("s"));
                else
                    Toast.makeText(this, "Connect to AIone first..", Toast.LENGTH_SHORT).show();
                break;
            case R.id.forward_button:
                if(bready==true)
                    sendMessage(new String("f"));
                else
                    Toast.makeText(this, "Connect to AIone first..", Toast.LENGTH_SHORT).show();
                break;
            case R.id.backward_button:
                if(bready==true)
                    sendMessage(new String("b"));
                else
                    Toast.makeText(this, "Connect to AIone first..", Toast.LENGTH_SHORT).show();
                break;
            case R.id.left_button:
                if(bready==true)
                    sendMessage(new String("l"));
                else
                    Toast.makeText(this, "Connect to AIone first..", Toast.LENGTH_SHORT).show();
                break;
            case R.id.right_button:
                if(bready==true)
                    sendMessage(new String("r"));
                else
                    Toast.makeText(this, "Connect to AIone first..", Toast.LENGTH_SHORT).show();
                break;

        }

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
