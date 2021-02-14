package util;

import share.Share;

public class ConsolePrinter extends Thread{
	public StringBuffer buffer = new StringBuffer();
	
	@Override
	public void run() {
		while(true) {
			if(buffer.length()>0) {
				String out=buffer.toString();
				buffer.setLength(0);
				System.out.print(out);
			}
			try {Thread.sleep(300);} catch (InterruptedException e) {/*e.printStackTrace();*/}
		}
	}
}
