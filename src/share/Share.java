package share;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import main.SyncThread;
import main.Task;
import net.AutoReconneectTimer;
import util.ConsolePrinter;
import util.TaskFFindTimer;

public class Share {
	public static SyncThread[] thpool=new SyncThread[Define.MaxTaskN];
	public static TaskFFindTimer[] thpool_TFFT=new TaskFFindTimer[Define.MaxTaskN];
	public static AutoReconneectTimer autoRecTimer=new AutoReconneectTimer();
	public static int TaskN;
	public static Task curentTask;
	public static boolean doneCpyTask,doneCpyTaskFFT;
	public static boolean pause;
	
	public static String deviceId="";
	public static Session session;
	public static boolean isBusy;
	public static boolean conState=false;
	
	public static String serverIp;
	public static String rootAddress;
	public static String UIserverIp;
	public static String UIserverIp_bypassed;
	public static String enableSC;
	public static String autoOptmz;
	
	public static WebSocketContainer container = ContainerProvider.getWebSocketContainer();
	public static String pathRoot;
	public static String optSys="linux";
	public static int tryConCnt=0;
	
	public static ConsolePrinter consolePrinter=new ConsolePrinter();
}
