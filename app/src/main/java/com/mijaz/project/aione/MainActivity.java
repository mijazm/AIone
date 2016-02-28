package com.mijaz.project.aione;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Activity;

public class MainActivity extends AppCompatActivity {

    ListView list;
    String[] application_title = {
            "Bluetooth RC",
            "NetBot"
    };

    String[] application_description = {
            "Use your phone as remote control!",
            "Control AIone from a webpage!"
    };

    Integer[] application_image = {
            R.drawable.bluetooth,
            R.drawable.internet
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CustomList adapter = new CustomList(MainActivity.this,application_title,application_description,application_image);

        list = (ListView)findViewById(R.id.application_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                switch(position){
                    case 0:
                        Intent BluetoothRemote = new Intent(getApplicationContext(), BluetoothRemote.class);
                        startActivity(BluetoothRemote);
                        break;
                    case 1:
                        Intent Netbot = new Intent(getApplicationContext(),NetBot.class);
                        startActivity(Netbot);

                }
            }
        });


    }
}
