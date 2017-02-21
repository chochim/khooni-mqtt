package in.huhuba.mqttclient;


import in.huhuba.paho.client.mqttv3.MqttClient;
import in.huhuba.paho.client.mqttv3.MqttClientPersistence;
import in.huhuba.paho.client.mqttv3.MqttDeliveryToken;
import in.huhuba.paho.client.mqttv3.MqttException;
import in.huhuba.paho.client.mqttv3.internal.wire.MqttPingReq;

/**
 * Created by rohit on 10/01/16.
 */
public class KhooniMqttClient extends MqttClient {

    public KhooniMqttClient(String serverUrl, String clientId, MqttClientPersistence persistence)
            throws MqttException {
        super(serverUrl, clientId, persistence);
    }

    public void ping() throws MqttException {
        MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
        MqttPingReq pingMsg = new MqttPingReq();
        aClient.comms.sendNoWait(pingMsg, token);
    }
}
