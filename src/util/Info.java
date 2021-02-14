package util;

import java.text.SimpleDateFormat;
import java.util.Date;

import share.Share;

public class Info {
	static SimpleDateFormat infoTimeFormat=new SimpleDateFormat("[HH:mm:ss");
	public static void out(String T) {
		Share.consolePrinter.buffer.append(infoTimeFormat.format(new Date())+" INFO]: "+T+"\n");
	}
	public static void wrn(String T) {
		Share.consolePrinter.buffer.append(infoTimeFormat.format(new Date())+" WARN]: "+T+"\n");
	}
	public static void err(String T) {
		Share.consolePrinter.buffer.append(infoTimeFormat.format(new Date())+" ERRO]: "+T+"\n");
	}
}
