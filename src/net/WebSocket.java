package net;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.filechooser.FileSystemView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import main.Main;
import main.Optimization;
import main.Task;
import share.Define;
import share.Share;
import share.Timepoint;
import util.BareBonesBrowserLaunch;
import util.FileOperation;
import util.Info;
import util.PathUtil;

@ClientEndpoint
public class WebSocket {
	boolean busyState;
	@OnOpen
    public void onOpen(Session session) throws IOException {
		if(!Share.deviceId.equals("")) Share.session.getBasicRemote().sendText("{\"packageType\":\"login\",\"connectionType\":\"server\",\"deviceId\":\""+Share.deviceId+"\"}");
		Share.conState=true;
		Share.tryConCnt=0;
        Info.out("Connected to skylark console successfully.");
    }

    @OnMessage
    public void onMessage(String dat,Session T) throws InterruptedException, IOException, ParseException {
    	//System.out.println(dat);
    	Gson gson = new Gson();
		JsonObject pkg=gson.fromJson(dat,JsonObject.class);
		if(!pkg.has("packageType"))	return;
		if(!pkg.has("deviceId")) {
			String pkgType=fom(pkg.get("packageType").toString());
			if(pkgType.equals("setDevId")){
				String newDevId=fom(pkg.get("newDevId").toString());
				Share.deviceId=newDevId;
				Info.out("Applied for new device address: "+newDevId);
			}
			return;
		}
		String pkgType=fom(pkg.get("packageType").toString());
		String deviceId=fom(pkg.get("deviceId").toString());
		switch(pkgType) {
			case "createTask":
				String newTaskName=fom(pkg.get("taskName").toString());
				String newTaskFrom=fom(pkg.get("taskFrom").toString());
				String newTaskTo=fom(pkg.get("taskTo").toString());
				int newTaskInterval=Integer.valueOf(fom(pkg.get("taskInterval").toString()));
				int newTaskId=Integer.valueOf(fom(pkg.get("taskId").toString()));
				int newTaskIoIndex=Integer.valueOf(fom(pkg.get("taskIoIndex").toString()));
				if(Share.optSys.equals("windows")) {
					newTaskTo=newTaskTo.replaceAll("/", "\\\\");
					newTaskFrom=newTaskFrom.replaceAll("/", "\\\\");
				}
				for(int i=0;i<Main.ids.size();i++) {
					int taskId=(int) Main.ids.get(i);
					if(Share.thpool[taskId].T.pathTo.equals(newTaskTo) && taskId!=newTaskId) {
						MsgPkg msgPkg=new MsgPkg();
						msgPkg.deviceId=Share.deviceId;
						msgPkg.msg="备份到的文件夹和其他任务冲突！\n请更换一个备份到的文件夹\n创建任务失败，请更正以上错误后重新创建。";
						T.getBasicRemote().sendText(gson.toJson(msgPkg).toString());
						return;
					}
				}
				
				if(newTaskId==-1) for(int i=0;i<Define.MaxTaskN;i++) if(!Main.ids.contains(i)) {
					newTaskId=i;
					break;
				}
				createTaskFile(newTaskName,newTaskFrom,newTaskTo,newTaskInterval,newTaskId,newTaskIoIndex);
				Main.unloadAllTask();
				Main.loadTask();
			break;
			case "getTaskList":
				TaskList taskList = new TaskList();
				for(int i=0;i<Main.ids.size();i++) {
					TaskListElem taskListElem=new TaskListElem();
					Task tmpT=Share.thpool[(int) Main.ids.get(i)].T;
					taskList.taskId.add(tmpT.id);
					taskList.taskFrom.add(tmpT.pathFrom);
					taskList.taskTo.add(tmpT.pathTo);
					taskList.taskInterval.add(tmpT.freq);
					taskList.taskIoIndexInput.add(tmpT.delay);
					taskListElem.taskName=tmpT.name;
					int st=tmpT.flotDat.size()-Define.flotPreserveN; if(st<0) st=0;
					for(int j=st;j<tmpT.flotDat.size();j++) taskListElem.flot.add(tmpT.flotDat.get(j));
					taskList.taskList.add(taskListElem);
				}
				taskList.deviceId=Share.deviceId;
				//System.out.println(gson.toJson(taskList).toString());
				try {T.getBasicRemote().sendText(gson.toJson(taskList).toString());} catch (IOException e1) {e1.printStackTrace();}
			break;
			case "listFolder":
				String curpath=fom(pkg.get("path").toString());
				FolderList folderList=new FolderList();
				folderList.deviceId=Share.deviceId;
				if(curpath.equals("/") && Share.optSys.equals("windows")) {
					FileSystemView sys = FileSystemView.getFileSystemView();
					File[] files = File.listRoots();
					for(int i=0;i<files.length;i++) folderList.list.add(files[i].getPath());
				}else {
					if(Share.optSys.equals("windows")) curpath=curpath.replaceAll("/", "\\\\");
					try {FileOperation.findCurFolder(curpath, folderList.list);} catch (IOException e) {e.printStackTrace();} catch (InterruptedException e) {e.printStackTrace();}
				}
				//System.out.println(curpath);
				try {T.getBasicRemote().sendText(gson.toJson(folderList).toString());} catch (IOException e) {e.printStackTrace();}
			break;
			case "updateFlot":
				TaskList taskList1 = new TaskList();
				for(int i=0;i<Main.ids.size();i++) {
					TaskListElem taskListElem=new TaskListElem();
					Task tmpT=Share.thpool[(int) Main.ids.get(i)].T;
					taskList1.taskId.add(tmpT.id);
					int st=tmpT.flotDat.size()-Define.flotPreserveN; if(st<0) st=0;
					for(int j=st;j<tmpT.flotDat.size();j++) taskListElem.flot.add(tmpT.flotDat.get(j));
					taskList1.taskList.add(taskListElem);
				}
				taskList1.deviceId=Share.deviceId;
				taskList1.packageType="updateFlot";
				//System.out.println(gson.toJson(taskList1).toString());
				try {T.getBasicRemote().sendText(gson.toJson(taskList1).toString());} catch (IOException e1) {e1.printStackTrace();}
			break;
			case "listTimePoints":
				int id=Integer.valueOf(fom(pkg.get("taskId").toString()));
				Task tmpT=Share.thpool[id].T;
				TimePointPkg timePointPkg = new TimePointPkg();
				timePointPkg.deviceId=Share.deviceId;
				timePointPkg.timepoints=Main.getTimePointList(id);
				T.getBasicRemote().sendText(gson.toJson(timePointPkg).toString());
			break;
			case "extract":
				busyState=true; sendBusyState(T);
				String to=fom(pkg.get("to").toString());
				int taskId=Integer.valueOf(fom(pkg.get("taskId").toString()));
				int tpId=Integer.valueOf(fom(pkg.get("tpId").toString()));
				Task task=Share.thpool[taskId].T;
				String outpath=to;
				List<Timepoint> timepoints=Main.getTimePointList(taskId);
				TreeMap<String,String> Fmap=new TreeMap<String,String>();
				for(int i=0;i<=tpId;i++) {
					Timepoint tmptp=timepoints.get(i);
					List Flist = new ArrayList<String>(); FileOperation.findFiles(task.pathTo+Define.sl+tmptp.path, 0, Flist);
					for(int j=0;j<Flist.size();j++) {
						String key=PathUtil.getRightPath(task.pathTo+Define.sl+tmptp.path, Flist.get(j).toString()), val=Flist.get(j).toString();
						Fmap.put(key, val);
					}
					String[] tmp1=FileOperation.readFile(task.pathTo+Define.sl+tmptp.path+Define.sl+Define.delListFileName).split("\\r\\n");
					for(int j=0;j<tmp1.length;j++) Fmap.remove(tmp1[j]);
				}
				List cpyList=new ArrayList<SS>();
				Iterator<Entry<String, String>> it=Fmap.entrySet().iterator();
				while (it.hasNext()) {
					SS tmp1=new SS();
				    Entry<String, String> E = it.next();
				    tmp1.a=E.getValue(); tmp1.b=outpath+Define.sl+E.getKey();
				    cpyList.add(tmp1); File tmpf= new File(tmp1.a);
				}
				for(int i=0;i<cpyList.size();i++) {
					SS tmpss=(SS) cpyList.get(i);
					FileOperation.cpyFile(tmpss.a, tmpss.b);
					File tmpf= new File(tmpss.a);
				}
				FileOperation.delFile(outpath+Define.sl+Define.delListFileName);
				busyState=false; sendBusyState(T);
			break;
			case "areYouBusy":
				sendBusyState(T);
			break;
			case "delTask":
				int delTaskId=Integer.valueOf(fom(pkg.get("taskId").toString()));
				FileOperation.delFile(Define.pathTaskbin+Define.sl+delTaskId+".txt");
				Main.unloadAllTask();
				Main.loadTask();
			break;	
			case "optimize":
				busyState=true; sendBusyState(T);
				int optimizeTaskId=Integer.valueOf(fom(pkg.get("taskId").toString()));
				Optimization.optimize(optimizeTaskId);
				busyState=false; sendBusyState(T);
			break;	
			case "goodbye":
				Share.enableSC="false";
				Main.saveCfg(Share.pathRoot+Define.sl+"config.json");
				Share.session.close();
			break;	
		}
    }
    public void sendBusyState(Session T) throws IOException {
    	Gson gson = new Gson();
    	BusyStatePkg busyStatePkg=new BusyStatePkg();
		busyStatePkg.deviceId=Share.deviceId;
		busyStatePkg.busyState=busyState;
		T.getBasicRemote().sendText(gson.toJson(busyStatePkg).toString());
    }
    public void createTaskFile(String newTaskName,String newTaskFrom,String newTaskTo,int newTaskInterval,int newTaskId,int newTaskIoIndex) throws IOException {
    	String cont=newTaskName+"\n"+newTaskId+"\n"+newTaskFrom+"\n"+newTaskTo+"\n"+newTaskInterval+"\n"+newTaskIoIndex;
    	FileOperation.writeFileTxt(Define.pathTaskbin+Define.sl+newTaskId+".txt", cont);
    }
    
    @OnClose
    public void onClose() throws InterruptedException {
    	Share.conState=false;
    	Info.wrn("Connection to skylark console terminated!");
		Thread.sleep(3000);
    }
    
    @OnError
    public void onError(Throwable t) throws InterruptedException {
    	Share.conState=false;
    	Thread.sleep(1000);
    }
    
    private String fom(String T) {if(T.length()<=2) return ""; return T.substring(1,T.length()-1);}
    public static void tryConnectConsole() throws InterruptedException {
    	if(Share.enableSC.equals("false")) {
    		Info.out("Disabled GUI module!");
    		return;
    	}
    	try {
    		//if(Share.session!=null) Share.session.close();
			Share.session = Share.container.connectToServer(WebSocket.class, URI.create(Share.serverIp));
		} catch (DeploymentException e) {
			Share.tryConCnt++;
			Info.err("Failed to connect to SkylarkConsole server!");
			Info.err("Will try again later, please contact author!");
		} catch (IOException e) {
			Share.tryConCnt++;
			Info.err("Failed to connect to SkylarkConsole server!");
			Info.err("Will try again later, please contact author!");
		}
    	/*if(Share.enableSC.equals("false") || Share.tryConCnt>Define.tryConnectTimes) {
    		Info.out("Disabled GUI module!");
    		return;
    	}
    	try {
			Share.session = Share.container.connectToServer(WebSocket.class, URI.create(Share.serverIp));
		} catch (DeploymentException e) {
			Share.tryConCnt++;
			if(Share.enableSC.equals("false") || Share.tryConCnt>Define.tryConnectTimes) {
				Info.err("Disabled GUI module, will no longger to try!");
				return;
			}
			Info.err("Failed to connect to SkylarkConsole server!");
			Info.err("Will try again later, please contact author!");
			Thread.sleep(3000);
			tryConnectConsole();
		} catch (IOException e) {
			Share.tryConCnt++;
			if(Share.enableSC.equals("false") || Share.tryConCnt>Define.tryConnectTimes) {
				Info.err("Disabled GUI module, will no longger to try!");
				return;
			}
			Info.err("Failed to connect to SkylarkConsole server!");
			Info.err("Will try again later, please contact author!");
			Thread.sleep(3000);
			tryConnectConsole();
		}*/
    }
}
class MsgPkg{
	String packageType="msg";
	String deviceId;
	String connectionType="server";
	String msg;
}
class BusyStatePkg{
	String packageType="busyState";
	String deviceId;
	String connectionType="server";
	boolean busyState;
}
class TimePointPkg{
	String packageType="timePoints";
	String deviceId;
	String connectionType="server";
	List<Timepoint> timepoints=new ArrayList<Timepoint>();;
}
class FolderList{
	String packageType="folderList";
	String deviceId;
	String connectionType="server";
	List list=new ArrayList<String>();
}
class TaskListElem{
	String taskName;
	List flot=new ArrayList<Double>();
}
class TaskList{
	String packageType="taskList";
	String deviceId;
	String connectionType="server";
	List taskId=new ArrayList<Integer>();
	List taskFrom=new ArrayList<String>();
	List taskTo=new ArrayList<String>();
	List taskInterval=new ArrayList<Integer>();
	List taskIoIndexInput=new ArrayList<Integer>();
	List taskList=new ArrayList<TaskListElem>();
}
class SS{String a,b;}