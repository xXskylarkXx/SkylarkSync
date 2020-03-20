package main;

import java.io.File;
import java.io.IOException;
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
	public static void main(String[] args) throws IOException, InterruptedException {
		/*Initialization*/
			System.out.print("\r\n-----------Author information-----------\r\n"+"Author	: Skylark\r\n" + "Contact	: QQ2991742773\r\n" + "E-mail	: 2991742773@qq.com\n"+"----------------------------------------\r\n");
			//FileOperation.readFile("config/xxx.txt");
		/*Load tasks*/
			List pathTaskFiles = new ArrayList<String>();
			FileOperation.findFiles(Define.pathTaskbin,0,pathTaskFiles);
			Share.TaskN=pathTaskFiles.size();
			for(int i=0;i<Share.TaskN;i++) {
				Task newT=new Task();
				String tmp=FileOperation.readFile((String) pathTaskFiles.get(i));;
				String[] T=tmp.split("\\r?\\n");
				if(T.length<6) {Info.err("One of your task configure file is unqualified!"); System.exit(0);}
				newT.name=T[0]; newT.id=Integer.parseInt(T[1]); newT.pathFrom=T[2]; newT.pathTo=T[3]; newT.freq=Integer.parseInt(T[4]); newT.delay=Integer.parseInt(T[5]);
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
				Scanner sc=new Scanner(System.in); String cmd=sc.nextLine();
				if(cmd.equals("extract")) {
					pauseAllTask();
					Info.out("Please input the task id that you want to extract: ");
					int id=Integer.parseInt(sc.nextLine());
					Task T=Share.thpool[id].T;
					Info.out("Extract to where: ");
					String outpath=sc.nextLine();
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
					for(int i=0;i<timepoints.size();i++) {Timepoint tmp1=(Timepoint) timepoints.get(i); System.out.println(tmp1.num+"  "+tmp1.time);}
					Info.out("Choose a timepoint which you want to extract,and input its number");
					int to=Integer.parseInt(sc.nextLine());
					Info.out("Processing...");
					TreeMap<String,String> Fmap=new TreeMap<String,String>();
					for(int i=0;i<=to;i++) {
						Timepoint tmptp=(Timepoint) timepoints.get(i);
						List Flist = new ArrayList<String>(); FileOperation.findFiles(T.pathTo+"\\"+tmptp.path, 0, Flist);
						for(int j=0;j<Flist.size();j++) {
							String key=PathUtil.getRightPath(T.pathTo+"\\"+tmptp.path, Flist.get(j).toString()), val=Flist.get(j).toString();
							Fmap.put(key, val);
						}
						String[] tmp1=FileOperation.readFile(T.pathTo+"\\"+tmptp.path+"\\"+Define.delListFileName).split("\\r\\n");
						for(int j=0;j<tmp1.length;j++) Fmap.remove(tmp1[j]);
						
					}
					System.out.print(">");
					double allSize=0,curExtra=0, progress=0; List cpyList=new ArrayList<SS>();
					Iterator<Entry<String, String>> it=Fmap.entrySet().iterator();
					while (it.hasNext()) {
						SS tmp1=new SS();
					    Entry<String, String> E = it.next();
					    tmp1.a=E.getValue(); tmp1.b=outpath+"\\"+E.getKey();
					    cpyList.add(tmp1); File tmpf= new File(tmp1.a);
					    allSize+=tmpf.length()/1073741824;
					}
					for(int i=0;i<cpyList.size();i++) {
						SS tmpss=(SS) cpyList.get(i);
						FileOperation.cpyFile(tmpss.a, tmpss.b);
						File tmpf= new File(tmpss.a);
						curExtra+=tmpf.length()/1073741824;
						double rate=curExtra/allSize;
						if(rate-progress>=0.01) {
							for(int j=0;j<(rate-progress)*100;j+=2) System.out.print(">");
							progress=rate; 
						}
					}
					System.out.println("");
					FileOperation.delFile(outpath+"\\"+Define.delListFileName);
					Info.out("Done extraction.\r\nPress any key to continue...");
					sc.nextLine();
					unpauseAllTask();
				}
				if(cmd.equals("pause")) pauseAllTask();
				if(cmd.equals("continue")) unpauseAllTask();
				if(cmd.equals("exit")) {
					pauseAllTask();
					System.exit(0);
				}
				if(cmd.equals("version")) Info.out(FileOperation.readFile("Version.txt"));
				if(cmd.equals("help")) Info.out("Executable commands:  pause  continue  exit  version  help");
			}
	}
	public static void outputMap(int T) {
	    Iterator<Entry<String, String>> it = Share.thpool[T].T.FileMD_new.entrySet().iterator();
	    while (it.hasNext()) {
	      Entry<String, String> entry = it.next();
	      Info.out(entry.getKey()+" "+entry.getValue());
	    }
	}
	public static boolean allPaused() {
		for(int i=0;i<Define.MaxTaskN-1;i++) if(Share.thpool[i]!=null && Share.thpool[i].running) return false;
		return true;
	}
	public static boolean allRnning() {
		for(int i=0;i<Define.MaxTaskN-1;i++) if(Share.thpool[i]!=null && !Share.thpool[i].running) return false;
		return true;
	}
	public static void pauseAllTask() throws InterruptedException {
		Share.pause=true;
		Info.out("Waiting all tasks be finished...");
		int tmp=0; while(!allPaused()) {Thread.sleep(1000); tmp++; System.out.print("."); if(tmp>=40) {tmp=0; System.out.println("");}}
		System.out.println(""); Info.out("All tasks were finished and shutdowned.");
	}
	public static void unpauseAllTask() throws InterruptedException {
		Share.pause=false;
		Info.out("Starting all tasks...");
		int tmp=0; while(!allRnning()) {Thread.sleep(100); tmp++; System.out.print("."); if(tmp>=38) {tmp=0; System.out.println("");}}
		System.out.println(""); Info.out("All tasks were started.");
	}
}
class SS{String a,b;}
