package util;

import main.Task;
import share.Share;

public class TaskFFindTimer  extends Thread{
	Task T=Share.curentTask;
	@Override
	public void run() {
		Share.doneCpyTaskFFT=true;
		while(true) {
			try {Thread.sleep(0,T.delay*10000);} catch (InterruptedException e) {/*e.printStackTrace();*/return;}
			T.active=true;
		}
	}
}
