package com.grofers.mqttclient;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rohit on 10/01/16.
 */
public class MqttServiceDelegate {
    public interface MessageHandler{
        public void handleMessage(String topic, byte[] payload);
    }

    public interface StatusHandler{
        public void handleStatus(MqttService.MQTTConnectionStatus status, String reason);
    }

    public static void startService(Context context){
        Intent svc = new Intent(context, MqttService.class);
        context.startService(svc);
    }

    public static void stopService(Context context){
        Intent svc = new Intent(context, MqttService.class);
        context.stopService(svc);
    }

    public static void publish(Context context, String topic, byte[] payload)
    {
        Intent actionIntent = new Intent(context, MqttService.class);
        actionIntent.setAction(MqttService.MQTT_PUBLISH_MSG_INTENT);
        actionIntent.putExtra(MqttService.MQTT_PUBLISH_MSG_TOPIC, topic);
        actionIntent.putExtra(MqttService.MQTT_PUBLISH_MSG, payload);
        context.startService(actionIntent);
    }

    public static class StatusReceiver extends BroadcastReceiver
    {
        private List<StatusHandler> statusHandlers = new ArrayList<StatusHandler>();

        public void registerHandler(StatusHandler handler){
            if(!statusHandlers.contains(handler)){
                statusHandlers.add(handler);
            }
        }

        public void unregisterHandler(StatusHandler handler){
            if(statusHandlers.contains(handler)){
                statusHandlers.remove(handler);
            }
        }

        public void clearHandlers(){
            statusHandlers.clear();
        }

        public boolean hasHandlers(){
            return statusHandlers.size() > 0;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle notificationData = intent.getExtras();
            MqttService.MQTTConnectionStatus statusCode =
                    MqttService.MQTTConnectionStatus.class.getEnumConstants()[notificationData.getInt(
                            MqttService.MQTT_STATUS_CODE)];
            String statusMsg = notificationData.getString(
                    MqttService.MQTT_STATUS_MSG);

            for(StatusHandler statusHandler : statusHandlers){
                statusHandler.handleStatus(statusCode, statusMsg);
            }
        }
    }

    public static class MessageReceiver extends BroadcastReceiver
    {
        private List<MessageHandler> messageHandlers = new ArrayList<MessageHandler>();

        public void registerHandler(MessageHandler handler){
            if(!messageHandlers.contains(handler)){
                messageHandlers.add(handler);
            }
        }

        public void unregisterHandler(MessageHandler handler){
            if(messageHandlers.contains(handler)){
                messageHandlers.remove(handler);
            }
        }

        public void clearHandlers(){
            messageHandlers.clear();
        }

        public boolean hasHandlers(){
            return messageHandlers.size() > 0;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Bundle notificationData = intent.getExtras();
            String topic = notificationData.getString(MqttService.MQTT_MSG_RECEIVED_TOPIC);
            byte[] payload  = notificationData.getByteArray(MqttService.MQTT_MSG_RECEIVED_MSG);
            Log.d("MessageHandlers","Total Handlers:"+messageHandlers.size());



            for(MessageHandler messageHandler : messageHandlers){
                messageHandler.handleMessage(topic, payload);
            }
        }
    }
}
