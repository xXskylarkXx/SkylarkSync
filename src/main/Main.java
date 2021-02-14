package main;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.text.ParseException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.util.Scanner;
import java.util.TreeMap;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import net.WebSocket;
import share.Define;
import share.Share;
import share.Timepoint;
import util.BareBonesBrowserLaunch;
import util.FileOperation;
import util.Info;
import util.Pars;
import util.PathUtil;
import util.TaskFFindTimer;

public class Main {
	public static List ids=new ArrayList<Integer>();
	public static void main(String[] args) throws ParseException, InterruptedException, ParseException, IOException, DeploymentException {
		/*Initialization*/
			Share.consolePrinter.start();
			Share.pathRoot=System.getProperty("user.dir");
			if(System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) {
				Share.optSys="windows";
				Define.sl="\\";
			}
			System.out.println(FileOperation.readFile(Share.pathRoot+"/lib/logo.txt"));
			System.out.println("<==============Type \"help\" or see READ_ME.pdf to learn more.==============>\r\n");
			readCfg(Share.pathRoot+Define.sl+"config.json");
			if(Share.enableSC.equals("true")) {
				while(!Share.conState) {
					WebSocket.tryConnectConsole();
					if(Share.conState) {
						readDevId(Share.pathRoot+Define.sl+"ID.txt");
						Share.session.getBasicRemote().sendText("{\"packageType\":\"login\",\"connectionType\":\"server\",\"deviceId\":\""+Share.deviceId+"\"}");
						Info.out("You may now access the GUI via website:\n"+"	"+Share.UIserverIp_bypassed+"/?id="+Share.deviceId);
						BareBonesBrowserLaunch.openURL(Share.UIserverIp_bypassed+"/?id="+Share.deviceId);
						Share.autoRecTimer.start();
					}
					
					if(Share.tryConCnt>Define.tryConnectTimes) {
						Share.autoRecTimer.start();
						break;
					}
				}
			}else{
				Info.out("Disabled GUI module!");
				Info.out("Try command: \"gui\" to enabled it.");
			}
		/*Load tasks*/
			loadTask();
		/*User interaction*/
			while(true) {
				boolean validCmd=false;
				Scanner sc=new Scanner(System.in); String cmd=sc.nextLine();
				if(cmd.equals("extract")) {
					validCmd=true;
					pauseAllTask();
					Info.out("Please input the task id that you want to extract: ");
					int id=Integer.parseInt(sc.nextLine());
					Task T=Share.thpool[id].T;
					Info.out("Extract to where: ");
					String outpath=sc.nextLine();
					List<Timepoint> timepoints=getTimePointList(id);
					for(int i=0;i<timepoints.size();i++) {Timepoint tmp1=timepoints.get(i); System.out.println(tmp1.num+"  "+tmp1.time);}
					Info.out("Choose a timepoint which you want to extract,and input its number:");
					int to=Integer.parseInt(sc.nextLine());
					Info.out("Processing...");
					TreeMap<String,String> Fmap=new TreeMap<String,String>();
					for(int i=0;i<=to;i++) {
						Timepoint tmptp=timepoints.get(i);
						List Flist = new ArrayList<String>(); FileOperation.findFiles(T.pathTo+Define.sl+tmptp.path, 0, Flist);
						for(int j=0;j<Flist.size();j++) {
							String key=PathUtil.getRightPath(T.pathTo+Define.sl+tmptp.path, Flist.get(j).toString()), val=Flist.get(j).toString();
							Fmap.put(key, val);
						}
						String[] tmp1=FileOperation.readFile(T.pathTo+Define.sl+tmptp.path+Define.sl+Define.delListFileName).split("\\r\\n");
						for(int j=0;j<tmp1.length;j++) Fmap.remove(tmp1[j]);
						
					}
					System.out.print(">");
					double allSize=0,curExtra=0, progress=0; List cpyList=new ArrayList<SS>();
					Iterator<Entry<String, String>> it=Fmap.entrySet().iterator();
					while (it.hasNext()) {
						SS tmp1=new SS();
					    Entry<String, String> E = it.next();
					    tmp1.a=E.getValue(); tmp1.b=outpath+Define.sl+E.getKey();
					    cpyList.add(tmp1); File tmpf= new File(tmp1.a);
					    allSize+=tmpf.length()/100;
					}
					for(int i=0;i<cpyList.size();i++) {
						SS tmpss=(SS) cpyList.get(i);
						FileOperation.cpyFile(tmpss.a, tmpss.b);
						File tmpf= new File(tmpss.a);
						curExtra+=tmpf.length()/100;
						double rate=curExtra/allSize;
						if(rate-progress>=0.01) {
							for(int j=0;j<(rate-progress)*100;j+=2) System.out.print(">");
							progress=rate; 
						}
					}
					System.out.println("");
					FileOperation.delFile(outpath+Define.sl+Define.delListFileName);
					Info.out("Done extraction.\r\nPress any key to continue...");
					sc.nextLine();
					unpauseAllTask();
				}
				if(cmd.equals("merge")) {
					validCmd=true;
					pauseAllTask();
					int from, to;
					Info.out("Please input the task id that you want to apply mergence:");
					int id=Integer.parseInt(sc.nextLine());
					Task T=Share.thpool[id].T;
					List<Timepoint> timepoints=getTimePointList(id);
					for(int i=0;i<timepoints.size();i++) {Timepoint tmp1=timepoints.get(i); System.out.println(tmp1.num+"  "+tmp1.time);}
					Info.out("Choose the timepoint of beginning,and input its number:");
					from=Integer.parseInt(sc.nextLine());
					Info.out("Choose the timepoint of ending,and input its number:");
					to=Integer.parseInt(sc.nextLine());
					Optimization.mergeTimePoint(id,from,to);
					Info.out("Done mergence.");
					unpauseAllTask();
				}
				if(cmd.equals("optimize")) {
					validCmd=true;
					pauseAllTask();
					Info.out("Please DO NOT SHUTDOWN during the optimization process otherwise your data may be lost!!!");
					Info.out("Please input the task id that you want to apply optimization:");
					int id=Integer.parseInt(sc.nextLine());
					Info.out("Optimizing...");
					int tmp=Optimization.optimize(id);
					Info.out("Done optimization, "+tmp+" timepoint were merged.");
					Info.out("Press any key to continue...");
					sc.nextLine();
					unpauseAllTask();
				}
				if(cmd.equals("pause")) {validCmd=true; pauseAllTask();}
				if(cmd.equals("continue")) {validCmd=true; unpauseAllTask();}
				if(cmd.equals("exit")) {
					validCmd=true;
					pauseAllTask();
					System.exit(0);
				}
				if(cmd.equals("version")) {validCmd=true; Info.out(FileOperation.readFile(Share.pathRoot+"/lib/version.txt"));}
				if(cmd.equals("help")) {validCmd=true;Info.out("\n"+FileOperation.readFile(Share.pathRoot+"/lib/help.txt"));}
				if(cmd.equals("gui")) {
					validCmd=true;
					Share.enableSC="true";
					Main.saveCfg(Share.pathRoot+Define.sl+"config.json");
					Info.out("You may now restart the software to enable GUI module.");
				}
				if(validCmd==false) Info.out("Invalid command, type \"help\" or see READ_ME.pdf to learn more.");
			}
	}
	public static void loadTask() throws IOException, InterruptedException {
		List pathTaskFiles = new ArrayList<String>();
		FileOperation.findFiles(Define.pathTaskbin,0,pathTaskFiles);
		Share.TaskN=pathTaskFiles.size();
		for(int i=0;i<Share.TaskN;i++) {
			Task newT=new Task();
			String tmp=FileOperation.readFile((String) pathTaskFiles.get(i));
			String[] T=tmp.split("\\r?\\n");
			if(T.length<6) {Info.err("One of your task configure file is unqualified! See READ_ME.pdf."); System.exit(0);}
			newT.name=T[0]; newT.id=Integer.parseInt(T[1]); newT.pathFrom=T[2]; newT.pathTo=T[3]; newT.freq=Integer.parseInt(T[4]); newT.delay=Integer.parseInt(T[5]); if(newT.delay>=0) newT.delay=1;
			newT.taskPath=(String) pathTaskFiles.get(i);
			ids.add(newT.id);
			newT.FileMD_new=new HashMap<String,String>(); newT.FileMD_old=new HashMap<String,String>();
			newT.flotDat=new ArrayList<Long>();
			String pathFlotF=newT.pathTo+Define.sl+Define.dataFolder+Define.sl+Define.flotDatFileName;
			File FFlot=new File(pathFlotF);
			if(FFlot.exists()) {
				String rawFlotDat=FileOperation.readFile(pathFlotF);
				String tmp1[]=rawFlotDat.split(" ");
				try{
					for(int j=0;j<tmp1.length;j++) newT.flotDat.add(Long.valueOf(tmp1[j]));
				}catch (NumberFormatException e) {
					FileOperation.delFile(pathFlotF);
				}
			}
			Share.curentTask=newT;
			
			Share.thpool[newT.id]=new SyncThread();
			Share.thpool[newT.id].start();
			Share.doneCpyTask=false;
			while(Share.doneCpyTask==false) Thread.sleep(1);
			
			Share.thpool_TFFT[newT.id]=new TaskFFindTimer();
			Share.thpool_TFFT[newT.id].start();
			Share.doneCpyTaskFFT=false;
			while(Share.doneCpyTaskFFT==false) Thread.sleep(1);
		}
	}
	public static void unloadAllTask() {
		for(int i=0;i<ids.size();i++) {
			Share.thpool[(int) ids.get(i)].interrupt();
			Share.thpool_TFFT[(int) ids.get(i)].interrupt();
			//Info.out(ids.get(i).toString());
		}
		ids.clear();
	}
	public static void readDevId(String pathDevId) throws IOException, InterruptedException {
		File F=new File(pathDevId);
		if(!F.exists()) {
			Info.out("Applying for new device address...");
			Share.session.getBasicRemote().sendText("{\"packageType\":\"requestNewDevId\"}");
			int cnt=0;
			while(Share.deviceId.equals("")) {
				if(cnt>100) {
					Info.err("Apply for device id time out, please contact author!");
					Share.enableSC="false";
					return;
				}
				Thread.sleep(50);
				cnt++;
			}
			FileOperation.writeFileTxt(pathDevId, Share.deviceId);
		}else Share.deviceId=FileOperation.readFile(pathDevId);
	}
	public static void readCfg(String pathCfg) throws JsonSyntaxException, IOException {
		Gson gson = new Gson();
		JsonObject cfg=gson.fromJson(FileOperation.readFile(pathCfg),JsonObject.class);
		Share.enableSC=Pars.json(cfg.get("Enable_SkylarkConsole").toString());
		Share.autoOptmz=Pars.json(cfg.get("Enable_automatic_optimization").toString());
		Share.rootAddress=Pars.json(cfg.get("SkylarkConsole_root_address").toString());
		Share.UIserverIp=Pars.json(cfg.get("SkylarkConsole_website").toString());
		Share.serverIp=Pars.json(cfg.get("SkylarkConsole_websocket_server_address").toString());
		try {
			Share.UIserverIp_bypassed=Share.UIserverIp.replace(Share.rootAddress,InetAddress.getByName(Share.rootAddress).getHostAddress());
		} catch (Exception e) {
			Info.err("Failed to resolve domain, please check the configuration!");
			Info.err("If the problom still remained unsolve, please contact author!");
			Share.enableSC="false";
		}
	}
	public static void saveCfg(String pathCfg) throws JsonSyntaxException, IOException {
		Config newcfg=new Config();
		newcfg.Enable_SkylarkConsole=Share.enableSC;
		newcfg.Enable_automatic_optimization=Share.autoOptmz;
		newcfg.SkylarkConsole_root_address=Share.rootAddress;
		newcfg.SkylarkConsole_website=Share.UIserverIp;
		newcfg.SkylarkConsole_websocket_server_address=Share.serverIp;
		Gson gson = new Gson();
		FileOperation.writeFileTxt(pathCfg, gson.toJson(newcfg).toString());
	}
	public static List getTimePointList(int taskid){
		Task T=Share.thpool[taskid].T;
		List timepoints=new ArrayList<Timepoint>(); 
		File Froot=new File(T.pathTo);
		String tmp[]=Froot.list();
		for(int i=0;i<tmp.length;i++) {
			if(tmp[i].equals(Define.dataFolder)) continue;
			Timepoint tmp1 = new Timepoint();
			String[] tmp0=tmp[i].split("__");
			if(tmp0.length!=2) continue;
			tmp1.num=Integer.parseInt(tmp0[0]); tmp1.path=tmp[i]; 
			String[] tmp2=tmp0[1].split("_");
			tmp1.time=tmp2[0]+" "+tmp2[1].replace(".", ":");
			timepoints.add(tmp1);
		}
		Collections.sort(timepoints,new Comparator<Timepoint>(){ public int compare(Timepoint  t0, Timepoint t1){ return (t0.num-t1.num);}});
		return timepoints;
	}
	public static void outputMap(int T) {
	    Iterator<Entry<String, String>> it = Share.thpool[T].T.FileMD_new.entrySet().iterator();
	    while (it.hasNext()) {
	      Entry<String, String> entry = it.next();
	      Info.out(entry.getKey()+" "+entry.getValue());
	    }
	}
	public static boolean allPaused() {
		for(int i=0;i<ids.size();i++) if(Share.thpool[(int) ids.get(i)].running) return false;
		return true;
	}
	public static boolean allRnning() {
		for(int i=0;i<ids.size();i++) if(!Share.thpool[(int) ids.get(i)].running) return false;
		return true;
	}
	public static void pauseAllTask() throws InterruptedException {
		Share.pause=true;
		Info.out("Waiting all tasks be finished...");
		int tmp=0; while(!allPaused()) {Thread.sleep(1000); tmp++; System.out.print("."); if(tmp>=40) {tmp=0; System.out.println("");}}
		if(tmp>0) System.out.println(""); Info.out("All tasks were finished and shutdowned.");
	}
	public static void unpauseAllTask() throws InterruptedException {
		Share.pause=false;
		Info.out("Starting all tasks...");
		//int tmp=0; while(!allRnning()) {Thread.sleep(1000); tmp++; System.out.print("."); if(tmp>=38) {tmp=0; System.out.println("");}}
		/*if(tmp>0) System.out.println(""); */Info.out("All tasks were started.");
	}
}
class SS{String a,b;}
