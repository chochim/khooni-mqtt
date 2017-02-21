package in.huhuba.mqttclient;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.grofers.mqttclient.R;

import java.sql.Timestamp;
import java.util.Date;

import in.huhuba.paho.client.mqttv3.logging.LogUtils;

import static in.huhuba.paho.client.mqttv3.logging.LogUtils.LOGD;

public class MainActivity extends AppCompatActivity
        implements MqttServiceDelegate.MessageHandler, MqttServiceDelegate.StatusHandler {
    private static final String TAG = LogUtils.makeLogTag(MainActivity.class);

    Button connectBtn, disconnectBtn, serverBtn;
    Context mContext;

    private MqttServiceDelegate.MessageReceiver msgReceiver;
    private MqttServiceDelegate.StatusReceiver statusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        connectBtn = (Button) findViewById(R.id.connect_button);
        disconnectBtn = (Button) findViewById(R.id.disconnect_button);
        serverBtn = (Button) findViewById(R.id.button_server);

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bindStatusReceiver();
                bindMessageReceiver();

                MqttServiceDelegate.startService(mContext);
            }
        });

        disconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MqttServiceDelegate.stopService(mContext);
                unbindMessageReceiver();
                unbindStatusReceiver();
            }
        });

        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent actionIntent = new Intent(mContext, MqttService.class);
                actionIntent.setAction(MqttService.MQTT_PUBLISH_MSG_INTENT);
                actionIntent.putExtra(MqttService.MQTT_PUBLISH_MSG_TOPIC, "ic");
                String message = "message bheja";
                actionIntent.putExtra(MqttService.MQTT_PUBLISH_MSG, message.getBytes() );
                actionIntent.putExtra(MqttService.MQTT_QA1, 1);
                mContext.startService(actionIntent);
            }
        });
    }

    private String getCurrentTimestamp(){
        return new Timestamp(new Date().getTime()).toString();
    }

    @Override
    public void handleMessage(String topic, byte[] payload) {
        String message = new String(payload);
        LOGD(TAG, "topic="+topic+", message="+message);
    }



    @Override
    public void handleStatus(MqttService.MQTTConnectionStatus status, String reason) {
        LOGD(TAG, "status = "+status+", reason = "+reason);
    }


    private void bindMessageReceiver(){
        msgReceiver = new MqttServiceDelegate.MessageReceiver();
        msgReceiver.registerHandler(this);
        registerReceiver(msgReceiver,
                new IntentFilter(MqttService.MQTT_MSG_RECEIVED_INTENT));
    }

    private void unbindMessageReceiver(){
        if(msgReceiver != null){
            msgReceiver.unregisterHandler(this);
            unregisterReceiver(msgReceiver);
            msgReceiver = null;
        }
    }

    private void bindStatusReceiver(){
        statusReceiver = new MqttServiceDelegate.StatusReceiver();
        statusReceiver.registerHandler(this);
        registerReceiver(statusReceiver,
                new IntentFilter(MqttService.MQTT_STATUS_INTENT));
    }

    private void unbindStatusReceiver(){
        if(statusReceiver != null){
            statusReceiver.unregisterHandler(this);
            unregisterReceiver(statusReceiver);
            statusReceiver = null;
        }
    }


    /* TODO: Check if it is working perfectly
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            NotificationManager n = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            n.cancel(MqttService.MQTT_NOTIFICATION_UPDATE);
        }
    }*/

    @Override
    protected void onDestroy() {
        unbindMessageReceiver();
        unbindStatusReceiver();
        super.onDestroy();
    }
}
