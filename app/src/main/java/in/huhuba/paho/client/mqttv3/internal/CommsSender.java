/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package in.huhuba.paho.client.mqttv3.internal;

import java.io.IOException;
import java.io.OutputStream;

import in.huhuba.paho.client.mqttv3.MqttException;
import in.huhuba.paho.client.mqttv3.MqttToken;
import in.huhuba.paho.client.mqttv3.internal.wire.MqttAck;
import in.huhuba.paho.client.mqttv3.internal.wire.MqttDisconnect;
import in.huhuba.paho.client.mqttv3.internal.wire.MqttOutputStream;
import in.huhuba.paho.client.mqttv3.internal.wire.MqttWireMessage;
import in.huhuba.paho.client.mqttv3.logging.LogUtils;

import static in.huhuba.paho.client.mqttv3.logging.LogUtils.LOGD;


public class CommsSender implements Runnable {
	private static final String TAG = LogUtils.makeLogTag(CommsSender.class);
	//Sends MQTT packets to the server on its own thread
	private boolean running 		= false;
	private Object lifecycle 		= new Object();
	private ClientState clientState = null;
	private MqttOutputStream out;
	private ClientComms clientComms = null;
	private CommsTokenStore tokenStore = null;
	private Thread 	sendThread		= null;
	
	public CommsSender(ClientComms clientComms, ClientState clientState, CommsTokenStore tokenStore, OutputStream out) {
		this.out = new MqttOutputStream(clientState, out);
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
	}
	
	/**
	 * Starts up the Sender thread.
	 */
	public void start(String threadName) {
		synchronized (lifecycle) {
			if (!running) {
				running = true;
				sendThread = new Thread(this, threadName);
				sendThread.start();
			}
		}
	}

	/**
	 * Stops the Sender's thread.  This call will block.
	 */
	public void stop() {
		final String methodName = "stop";
		
		synchronized (lifecycle) {
			//@TRACE 800=stopping sender
			LOGD(TAG, methodName+" 800");
			if (running) {
				running = false;
				if (!Thread.currentThread().equals(sendThread)) {
					try {
						// first notify get routine to finish
						clientState.notifyQueueLock();
						// Wait for the thread to finish.
						sendThread.join();
					}
					catch (InterruptedException ex) {
					}
				}
			}
			sendThread=null;
			//@TRACE 801=stopped
			LOGD(TAG, methodName+" 801");
		}
	}
	
	public void run() {
		final String methodName = "run";
		MqttWireMessage message = null;
		while (running && (out != null)) {
			try {
				message = clientState.get();
				if (message != null) {
					//@TRACE 802=network send key={0} msg={1}
					LOGD(TAG, methodName+" 802");

					if (message instanceof MqttAck) {
						out.write(message);
						out.flush();
					} else {
						MqttToken token = tokenStore.getToken(message);
						// While quiescing the tokenstore can be cleared so need 
						// to check for null for the case where clear occurs
						// while trying to send a message.
						if (token != null) {
							synchronized (token) {
								out.write(message);
								try {
									out.flush();
								} catch (IOException ex) {
									// The flush has been seen to fail on disconnect of a SSL socket
									// as disconnect is in progress this should not be treated as an error
									if (!(message instanceof MqttDisconnect)) {
										throw ex;
									}
								}
								clientState.notifySent(message);
							}
						}
					}
				} else { // null message
					//@TRACE 803=get message returned null, stopping}
					LOGD(TAG, methodName+" 803");

					running = false;
				}
			} catch (MqttException me) {
				handleRunException(message, me);
			} catch (Exception ex) {		
				handleRunException(message, ex);	
			}
		} // end while
		
		//@TRACE 805=<
		LOGD(TAG, methodName+" 805");

	}

	private void handleRunException(MqttWireMessage message, Exception ex) {
		final String methodName = "handleRunException";
		//@TRACE 804=exception
		LOGD(TAG, methodName+" 804");
		MqttException mex;
		if ( !(ex instanceof MqttException)) {
			mex = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ex);
		} else {
			mex = (MqttException)ex;
		}

		running = false;
		clientComms.shutdownConnection(null, mex);
	}
}
