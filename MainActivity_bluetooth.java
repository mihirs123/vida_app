package com.example.vidaapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    int REQUEST_ENABLE_BT=1;

    Button button,show,sendbtn;
    BluetoothAdapter obj;
    Intent btenable;
    ListView listview;
    BluetoothDevice btarray[];
    TextView textview,stat;
    String signal;
    EditText writemsg;
    SendReceive sendreceive;
    private static final String APP_NAME="vidaapp";
    private static final UUID MY_UUID=UUID.fromString("fc2d2628-9a5b-4ecc-8683-262e028f665e");
    static final int STATE_LISTENING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;
    public static final String EXTRA_MESSAGE = "com.example.vidaapp.MESSAGE";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.bton);
        show = (Button) findViewById(R.id.btshow);
        obj=BluetoothAdapter.getDefaultAdapter();
        textview= (TextView) findViewById(R.id.msg);
        sendbtn = (Button) findViewById(R.id.send);
        writemsg = (EditText) findViewById((R.id.write));
        stat= (TextView) findViewById(R.id.status);





        btenable=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        listview= (ListView) findViewById(R.id.listv);


        if(obj==null)
        {
            Toast.makeText(getApplicationContext(),"Bluetooth is not supported on this device",Toast.LENGTH_LONG).show();
        }
        else
        {
            if(!obj.isEnabled())
            {
                startActivityForResult(btenable,REQUEST_ENABLE_BT);
            }
        }

        buttonshowmethod();
        stat.setText(signal);




    }

    private void buttonshowmethod() {
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> bt = obj.getBondedDevices();
                String[] strings = new String[bt.size()];
                btarray=new BluetoothDevice[bt.size()];
                int index = 0;
                if (bt.size() == 0) {
                    Toast.makeText(getApplicationContext(), "no paired devices", Toast.LENGTH_LONG).show();
                } else {
                    for (BluetoothDevice devices : bt) {
                        btarray[index]=devices;
                        strings[index] = devices.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    listview.setAdapter(arrayAdapter);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass clientclass=new ClientClass(btarray[position]);
                clientclass.start();
                stat.setText("connecting");
            }
        });
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string= String.valueOf(writemsg.getText());
                sendreceive.write(string.getBytes());

            }
        });
    }
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage( Message msg) {
            switch(msg.what)
            {
                case STATE_LISTENING:
                {
                    stat.setText("listening");

                    break;
                }
                case STATE_CONNECTING:
                {
                    stat.setText("connecting");

                    break;
                }
                case STATE_CONNECTED:
                {
                    stat.setText("connected");
                    break;
                }
                case STATE_CONNECTION_FAILED:
                {
                    stat.setText("connection failed");
                    break;
                }
                case STATE_MESSAGE_RECEIVED:
                {
                    byte[] readbuffer= (byte[]) msg.obj;
                    String tempmag=new String(readbuffer,0,msg.arg1);
                    textview.setText(tempmag);
                    signal=tempmag;
                    stat.setText(signal);
                    
                        try {
                            SmsManager smgr = SmsManager.getDefault();
                            smgr.sendTextMessage("9403468717".toString(), null, "hii".toString(), null, null);
                            Toast.makeText(MainActivity.this, "SMS Sent Successfully", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "SMS Failed to Send, Please try again", Toast.LENGTH_SHORT).show();
                        }



                    



                    break;
                }

            }
            return false;
        }
    });



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Bluetooth is enabled", Toast.LENGTH_LONG).show();
            } else if (requestCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Bluetooth is not enabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class ServerClass extends Thread
    {
        private BluetoothServerSocket serverSocket;
        public ServerClass()
        {
            try {
                serverSocket= obj.listenUsingInsecureRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            BluetoothSocket socket=null;
            while(socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket=serverSocket.accept();


                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);

                }
                if(socket!=null)
                {

                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendreceive=new SendReceive(socket);
                    sendreceive.start();
                    break;
                }
            }
            Log.i("check","afsffsaadf");
        }



    }
    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;


        public ClientClass(BluetoothDevice device1) {
            device = device1;
        }
        public void run() {

            while (socket == null) {
                try {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                } catch (IOException e) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                    e.printStackTrace();
                }
                if(socket!=null) {
                    try {
                        socket.connect();
                        sendreceive=new SendReceive(socket);
                        sendreceive.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        }



    }
    private class SendReceive extends Thread
    {
        private final BluetoothSocket bluetoothsocket;
        private final InputStream inputstream;
        private final OutputStream outputstream;
        public SendReceive(BluetoothSocket socket)
        {
            bluetoothsocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;
            try {
                tempIn=bluetoothsocket.getInputStream();
                tempOut=bluetoothsocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }
            inputstream=tempIn;
            outputstream=tempOut;


        }
        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;
            while(true)
            {
                try {
                    bytes=inputstream.read(buffer);

                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes)
        {
            try {
                outputstream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }




}

