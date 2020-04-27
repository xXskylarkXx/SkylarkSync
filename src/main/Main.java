package main;

import java.io.File;
import java.io.IOException;
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
import java.util.Scanner;
import java.util.TreeMap;

import share.Define;
import share.Share;
import share.Timepoint;
import util.FileOperation;
import util.Info;
import util.PathUtil;
import util.TaskFFindTimer;

public class Main {
	public static List ids=new ArrayList<Integer>();
	public static void main(String[] args) throws ParseException, InterruptedException, ParseException, IOException {
		/*Initialization*/
			if(System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1) Define.sl="\\";
			System.out.println(FileOperation.readFile("logo.txt"));
			System.out.println("<==============Type \"help\" or see READ_ME.pdf to learn more.==============>\r\n");
			//FileOperation.readFile("config/xxx.txt");
		/*Load tasks*/
			List pathTaskFiles = new ArrayList<String>();
			FileOperation.findFiles(Define.pathTaskbin,0,pathTaskFiles);
			Share.TaskN=pathTaskFiles.size();
			for(int i=0;i<Share.TaskN;i++) {
				Task newT=new Task();
				String tmp=FileOperation.readFile((String) pathTaskFiles.get(i));;
				String[] T=tmp.split("\\r?\\n");
				if(T.length<6) {Info.err("One of your task configure file is unqualified! See READ_ME.pdf."); System.exit(0);}
				newT.name=T[0]; newT.id=Integer.parseInt(T[1]); newT.pathFrom=T[2]; newT.pathTo=T[3]; newT.freq=Integer.parseInt(T[4]); newT.delay=Integer.parseInt(T[5]);
				ids.add(newT.id);
				newT.FileMD_new=new HashMap<String,String>(); newT.FileMD_old=new HashMap<String,String>();
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
				if(cmd.equals("version")) {validCmd=true; Info.out(FileOperation.readFile("Version.txt"));}
				if(cmd.equals("help")) {
					validCmd=true;
					System.out.print("\r\n-----------Author information-----------\r\n"+"Author	: Skylark\r\n" + "Contact	: QQ2991742773\r\n" + "E-mail	: 2991742773@qq.com\n"+"----------------------------------------\r\n\r\n");
					System.out.println("Available commands:\n        extract\n        optimize\n        merge\n        pause\n        continue\n        exit\n        version\n        help");
				}
				if(validCmd==false) Info.out("Invalid command, type \"help\" or see READ_ME.pdf to learn more.");
			}
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
