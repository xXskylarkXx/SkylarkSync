package share;

import main.SyncThread;
import main.Task;
import util.TaskFFindTimer;

public class Share {
	public static SyncThread[] thpool=new SyncThread[Define.MaxTaskN];
	public static TaskFFindTimer[] thpool_TFFT=new TaskFFindTimer[Define.MaxTaskN];
	public static int TaskN;
	public static Task curentTask;
	public static boolean doneCpyTask,doneCpyTaskFFT;
	public static boolean pause;
}
