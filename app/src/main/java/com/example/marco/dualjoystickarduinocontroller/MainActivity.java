package com.example.marco.dualjoystickarduinocontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements JoyStick.JoyStickListener {

    /*Steps Guideline
     1)Open a bluetooth connection
     2) Send data
     3) Listen for incoming data
     4)Close the connection

     Notes
     - Step 2) and 3) do not have to occur in that order and
       either step 2) or step 3) may be omitted but not both
       to have a successful communication between Android and Arduino
     - Step 1) and 4) must happen at the beginning and the end,
       to establish a connection and close it
     */

    //variables used in opening and closing a bluetooth connection
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice mmDevice;
    BluetoothSocket mmSocket;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JoyStick joyStickLeftRight = (JoyStick) findViewById(R.id.joy1);
        JoyStick joyStickUpDown = (JoyStick) findViewById(R.id.joy2);

        joyStickLeftRight.setListener(this);
        joyStickUpDown.setListener(this);

        //Keeps joystick in last position
//        joyStickLeftRight.enableStayPut(true);
//        joyStickUpDown.enableStayPut(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem connectArduino = menu.findItem(R.id.connect_arduino);
        MenuItem disconnectArduino = menu.findItem(R.id.disconnect_arduino);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.connect_arduino:
                connectArduino();
                return true;
            case R.id.disconnect_arduino:
                disconnectArduino();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void connectArduino() {
        //method successful if blinking LED on bluetooth module
        //stops blinking or turns to solid color

        //return value of this will be null if the device does not have bluetooth capabilities
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //the bluetoothAdapter checks status of bluetooth on device
        //and if bluetooth is disabled, it can request that it be turned on
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
            Toast.makeText(getApplicationContext(), "ENABLED Bluetooth", Toast.LENGTH_LONG).show();
        }

        // get a reference to our Arduino’s bluetooth device
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-06")) {
                    mmDevice = device;
                    Toast.makeText(getApplicationContext(), "Ref to Arduino SUCCESSFUL", Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }
        //connect to arduino
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Connection to Arduino FAILED", Toast.LENGTH_LONG).show();
        }
    }

    void disconnectArduino() {
        //method successful if LED on bluetooth module is blinking
        try {
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Failed to disconnect Bluetooth", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public void onMove(JoyStick joyStick, double angle, double power) {
        //for 180 degree motors
        //when the angle is more than 180
        //have servo move in opposite direction
//        if (angle > 180) {
//            angle = 360 - angle;
//        }
        //turn angle into degrees
        angle = joyStick.getAngleDegrees();
        //typecast angle from double to int
        int intAngle = (int) angle;
        //typecast angle from int to char
        char angleInChar = '2';
        if ((intAngle <= 60) || (intAngle >= 181 && intAngle < 241)) {
            angleInChar = '1'; //servo pointing right
        } else if ((intAngle >= 121 && intAngle < 181) || (intAngle >= 181 && intAngle < 241)) {
            angleInChar = '3'; //servo pointing left
        } else {
            angleInChar = '2'; //servo pointing straight
        }


        switch (joyStick.getId()) {
            case R.id.joy1:
                //left right
                sendServoAngle(angleInChar);
                break;
            case R.id.joy2:
                //up down
                sendServoAngle(angleInChar);
                break;
        }
    }


    void sendServoAngle(char servoData) {
        try {
            //turn the string containing the angle into bytes
//            byte[] messageBuffer = servoData.getBytes();
            //transmit the message via bluetooth
            mmOutputStream.write(servoData);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error sending joystick data", Toast.LENGTH_LONG).show();
        }
    }
}
