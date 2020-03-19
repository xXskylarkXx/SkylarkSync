package share;

import main.SyncThread;
import main.Task;

public class Share {
	public static SyncThread[] thpool=new SyncThread[Define.MaxTaskN];
	public static int TaskN;
	public static Task curentTask;
	public static boolean doneCpyTask=false;
	public static boolean pause;
}
