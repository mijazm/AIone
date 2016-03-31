/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package com.mijaz.project.aione;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.UUID;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class PocketSphinxActivity extends Activity implements RecognitionListener,AdapterView.OnItemSelectedListener
{
    private static final String DIGITS_SEARCH = "digits";
    private SpeechRecognizer recognizer;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    HttpURLConnection c;
    char command;

    Thread workerThread;

    boolean bready=false;
    Boolean stopWorker = false;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate(Bundle state)
    {
        super.onCreate(state);
        setContentView(R.layout.sphinx_main);

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

        ((TextView) findViewById(R.id.caption_text)).setText("Preparing the recognizer");

        try
        {
            Assets assets = new Assets(PocketSphinxActivity.this);
            File assetDir = assets.syncAssets();
            setupRecognizer(assetDir);
        }
        catch (IOException e)
        {
            // oops
        }

        ((TextView) findViewById(R.id.caption_text)).setText("Say up, down, left, right, forwards, backwards");

        reset();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis)
    {
        if (hypothesis != null)
        {
            String text = hypothesis.getHypstr();
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onResult(Hypothesis hypothesis)
    {
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {

    }

    @Override
    public void onBeginningOfSpeech()
    {
    }

    @Override
    public void onEndOfSpeech()
    {
        reset();
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        File modelsDir = new File(assetsDir, "models");

        recognizer = defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                        // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setRawLogDir(assetsDir)

                        // Threshold to tune for keyphrase to balance between false alarms and misses
                .setKeywordThreshold(1e-45f)

                        // Use context-independent phonetic search, context-dependent is too slow for mobile
                .setBoolean("-allphone_ci", true)

                .getRecognizer();
        recognizer.addListener(this);

        File digitsGrammar = new File(assetsDir, "digits.gram");
        recognizer.addKeywordSearch(DIGITS_SEARCH, digitsGrammar);
    }

    private void reset()
    {
        recognizer.stop();
        recognizer.startListening(DIGITS_SEARCH);
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