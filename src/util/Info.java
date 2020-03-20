package util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Info {
	static SimpleDateFormat infoTimeFormat=new SimpleDateFormat("[HH:mm:ss]");
	public static void out(String T) {
		System.out.println(infoTimeFormat.format(new Date())+"[INFO]: "+T);
	}
	public static void wrn(String T) {
		System.out.println(infoTimeFormat.format(new Date())+"[WARN]: "+T);
	}
	public static void err(String T) {
		System.out.println(infoTimeFormat.format(new Date())+"[ERRO]: "+T);
	}
}
