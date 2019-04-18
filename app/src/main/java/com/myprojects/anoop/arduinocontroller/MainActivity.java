package com.myprojects.anoop.arduinocontroller;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public final String ACTION_USB_PERMISSION = "com.myprojects.anoop.arduinocontroller.USB_PERMISSION";


    Button startButton,  clearButton, stopButton;
    ImageButton sendButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    private MessageAdapter messageAdapter;
    private ListView messagesView;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                if (!data.isEmpty()) {
                    tvAppend(textView, data);

                    MemberData newMemberData = new MemberData("Arduino", "#3F51B5");
                    boolean belongsToCurrentUser = false;
                    final Message message = new Message(data, newMemberData, belongsToCurrentUser);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageAdapter.add(message);
                            messagesView.setSelection(messagesView.getCount() - 1);
                        }
                    });
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }

    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        messageAdapter = new MessageAdapter(this);
        messagesView = (ListView) findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);

        MemberData data = new MemberData();

        new UsbSerialInterface.UsbReadCallback() {
            @Override
            public void onReceivedData(byte[] bytes) {
                //do something with bytes
                String data = null;
                try {
                    data = new String(bytes, "UTF-8");
                    if (!data.isEmpty()) {
                        tvAppend(textView, data);

                        MemberData newMemberData = new MemberData("Arduino", "#3F51B5");
                        boolean belongsToCurrentUser = false;
                        final Message message = new Message(data, newMemberData, belongsToCurrentUser);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                messageAdapter.add(message);
                                messagesView.setSelection(messagesView.getCount() - 1);
                            }
                        });
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public void onClickStart(View view) {

        System.out.println("In onclickStart");
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        System.out.println(usbDevices.isEmpty());
        boolean v = usbDevices.isEmpty();
        textView.setText(String.valueOf(v));
        messageAdapter.clear();

        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();

                int deviceVID = device.getVendorId();

                System.out.println(deviceVID);
                textView.setText(String.valueOf(deviceVID));
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;

                    MemberData newMemberData = new MemberData("Arduino","#3F51B5");
                    boolean belongsToCurrentUser = false;
                    final  Message message = new Message("Please Connect Arduino",newMemberData, belongsToCurrentUser);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            messageAdapter.add(message);
                            messagesView.setSelection(messagesView.getCount() - 1);
                        }
                    });
                }

                if (!keep)
                    break;
            }
        }
        else {

            MemberData newMemberData = new MemberData("Arduino","#3F51B5");
            boolean belongsToCurrentUser = false;
            final  Message message = new Message("Please Connect USB- Device",newMemberData, belongsToCurrentUser);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageAdapter.add(message);
                    messagesView.setSelection(messagesView.getCount() - 1);
                }
            });
        }


    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();

        if(!string.isEmpty()){
            serialPort.write(string.getBytes());
            tvAppend(textView, "\nData Sent : " + string + "\n");

            MemberData newMemberData = new MemberData(string,"#3F51B5");
            boolean belongsToCurrentUser = true;
            final  Message message = new Message(string,newMemberData, belongsToCurrentUser);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageAdapter.add(message);
                    messagesView.setSelection(messagesView.getCount() - 1);
                }
            });
            editText.setText("");
        }


    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");

    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.setText(ftext);
            }
        });
    }
}
