/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */

package in.huhuba.paho.client.mqttv3;

import java.util.Timer;
import java.util.TimerTask;

import in.huhuba.paho.client.mqttv3.internal.ClientComms;
import in.huhuba.paho.client.mqttv3.logging.LogUtils;

import static in.huhuba.paho.client.mqttv3.logging.LogUtils.LOGD;

/**
 * Default ping sender implementation
 *
 * <p>This class implements the {@link IMqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
public class TimerPingSender implements MqttPingSender {
	private static final String CLASS_NAME = TimerPingSender.class.getName();
	private static final String TAG = LogUtils.makeLogTag(TimerPingSender.class);

	private ClientComms comms;
	private Timer timer;

	public void init(ClientComms comms) {
		if (comms == null) {
			throw new IllegalArgumentException("ClientComms cannot be null.");
		}
		this.comms = comms;
	}

	public void start() {
		final String methodName = "start";		
		String clientid = comms.getClient().getClientId();
		
		//@Trace 659=start timer for client:{0}
        LOGD(TAG, methodName+" 659");
				
		timer = new Timer("MQTT Ping: " + clientid);
		//Check ping after first keep alive interval.
		timer.schedule(new PingTask(), comms.getKeepAlive());
	}

	public void stop() {
		final String methodName = "stop";
        LOGD(TAG, methodName+" 661");
		if(timer != null){
			timer.cancel();
		}
	}

	public void schedule(long delayInMilliseconds) {
		timer.schedule(new PingTask(), delayInMilliseconds);		
	}
	
	private class PingTask extends TimerTask {
		private static final String methodName = "PingTask.run";
		
		public void run() {
			//@Trace 660=Check schedule at {0}
            LOGD(TAG, methodName+" 660");
			comms.checkForActivity();			
		}
	}
}
