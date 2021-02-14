package net;

import java.io.IOException;

import share.Share;

public class AutoReconneectTimer extends Thread{
	@Override
	public void run() {
		while(Share.enableSC.equals("true")) {
			if(Share.conState==false) {
				 try {WebSocket.tryConnectConsole();} catch (InterruptedException e) {e.printStackTrace();}
				 if(Share.conState)
					 try {Share.session.getBasicRemote().sendText("{\"packageType\":\"login\",\"connectionType\":\"server\",\"deviceId\":\""+Share.deviceId+"\"}");} catch (IOException e) {e.printStackTrace();}
			}
			try {Thread.sleep(1000*10);} catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}
