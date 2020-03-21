package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map.Entry;

import share.Define;
import share.Share;
import share.Timepoint;
import util.FileOperation;
import util.Info;
import util.PathUtil;
import util.TimeUtil;

public class SyncThread extends Thread{
	public Task T=Share.curentTask;
	public boolean running,stop=false,cnt;
	@Override
	public void run() {
		Share.doneCpyTask=true;
		boolean firstload=true;
		Info.out("Task"+T.id+"("+T.name+")"+" loaded successfully");
		try {Thread.sleep(10000);} catch (InterruptedException e) {e.printStackTrace();}
		while(!stop) {
			/*Sleep*/
			running=false;
			if(!firstload) {
				for(int i=0;i<(T.freq*60000)/1000;i++) {
					cnt=false;
					if(Share.pause) {cnt=true; break;}
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
				if(cnt) {
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
					continue;
				}
			}else{
				firstload=false;
				if(Share.pause) {
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
					continue;
				}
			}
			T.FileMD_new.clear(); T.FileMD_old.clear(); System.gc();
			/*Main thread interaction*/
				running=true;
				Info.out("Task"+T.id+"("+T.name+")"+" Start synchronize...");
			/*Synchronization*/
				/*Collect data*/
				try {FileOperation.writeFile(T.pathTo+Define.sl+Define.dataFolder+Define.sl+"tmp"+Define.sl+"creator.txt", "");} catch (IOException e1) {e1.printStackTrace();}
				T.FileMD_new.clear();
				Info.out("Task"+T.id+"("+T.name+")"+" Scanning files...");
				try {try {getNewFileMD(T.pathFrom,0,T.FileMD_new,1);} catch (InterruptedException e) {e.printStackTrace();}} catch (IOException e1) {e1.printStackTrace();}
				String path_file_timepointN=T.pathTo+Define.sl+Define.dataFolder+Define.sl+"TimePointN.txt";
				File file_timepointN=new File(path_file_timepointN);
				if(!file_timepointN.exists()) {
					try {FileOperation.writeFileTxt(path_file_timepointN, "0");} catch (IOException e) {e.printStackTrace();}
					Info.wrn("Task"+T.id+"("+T.name+")"+" TimePointN.txt doesn't exists, created a new one.");
				}
				try {T.timepointN=Integer.parseInt(FileOperation.readFile(path_file_timepointN));} catch (NumberFormatException | IOException e3) {e3.printStackTrace();}
				File file_FMD=new File(T.pathTo+Define.sl+Define.dataFolder+Define.sl+"FilesLastModifiedDate.txt");
				if(!file_FMD.exists()) {
					//Info.wrn("Task"+T.id+"("+T.name+")"+" 0");
					try {FileOperation.writeFile(T.pathTo+Define.sl+Define.dataFolder+Define.sl+"FilesLastModifiedDate.txt",mapToString(T.FileMD_new));} catch (IOException e) {e.printStackTrace();}
					Info.wrn("Task"+T.id+"("+T.name+")"+" FilesLastModifiedDate.txt doesn't exists, created a new one.");
				}else try {freadFileMD(T.pathTo+Define.sl+Define.dataFolder+Define.sl+"FilesLastModifiedDate.txt",T.FileMD_old);} catch (IOException | InterruptedException e2) {e2.printStackTrace();}
				Info.out("Task"+T.id+"("+T.name+")"+" File scan finished.");
				/*Compare files*/
				Info.out("Task"+T.id+"("+T.name+")"+" Copying files...");
				String nowTime=TimeUtil.timeFormat.format(new Date());
				try {FileOperation.writeFileTxt(T.pathTo+Define.sl+T.timepointN+"__"+nowTime+Define.sl+Define.delListFileName, "");} catch (IOException e1) {e1.printStackTrace();}
				//Info.wrn("Task"+T.id+"("+T.name+")"+" 1");
				try {FileOperation.writeFileTxt(path_file_timepointN, String.valueOf(T.timepointN+1));} catch (IOException e) {e.printStackTrace();}
				String curKey="", curVal="" ,resu="";
				int totalCpy=0; long totalCpySize=0; File tmpf0;
				for (Map.Entry<String, String> entry : T.FileMD_new.entrySet()) {
					//limitCPU_usage(1);
				    curKey=entry.getKey(); curVal=entry.getValue();
				    resu=T.FileMD_old.get(curKey);
				    String pathDest=T.pathTo+Define.sl+T.timepointN+"__"+nowTime+Define.sl+curKey;
				    File tmpF=new File(T.pathFrom+Define.sl+curKey);
				    if(curVal.equals(Define.occupied)) {Info.wrn("Task"+T.id+"("+T.name+") "+tmpF.getName()+" were locked by other programme, skipped it."); continue;}
				    if(resu==null) {/*If this file doesn't exists*/
				    	FileOperation.cpyFile(T.pathFrom+Define.sl+curKey,pathDest);
				    	tmpf0=new File(T.pathFrom+Define.sl+curKey);
				    	totalCpySize+=tmpf0.length();
				    	totalCpy++;
				    	continue;
				    }
				    if(!resu.equals(curVal)) {/*If this file were modified*/
				    	FileOperation.cpyFile(T.pathFrom+Define.sl+curKey,pathDest);
				    	tmpf0=new File(T.pathFrom+Define.sl+curKey);
				    	totalCpySize+=tmpf0.length();
				    	totalCpy++;
				    	continue;
				    }
				}
				Info.out("Task"+T.id+"("+T.name+")"+" Copied "+totalCpy+" files ("+String.format("%.1f", totalCpySize/1073741824.0)+"GB) in total, done.");
				String delFileList="";
				Info.out("Task"+T.id+"("+T.name+")"+" Saving task data...");
				for (Map.Entry<String, String> entry : T.FileMD_old.entrySet()) {
					//limitCPU_usage(1);
					curKey=entry.getKey(); curVal=entry.getValue();
				    resu=T.FileMD_new.get(curKey);
				    if(resu==null) {/*If this file were deleted*/
				    	delFileList+=(curKey+"\r\n");
				    	continue;
				    }
				}
				/*Synchronized ,saving data*/
				try {FileOperation.writeFileTxt(T.pathTo+Define.sl+T.timepointN+"__"+nowTime+Define.sl+Define.delListFileName, delFileList);} catch (IOException e1) {e1.printStackTrace();}
				try {FileOperation.writeFileTxt(T.pathTo+Define.sl+Define.dataFolder+Define.sl+"FilesLastModifiedDate.txt",mapToString(T.FileMD_new));} catch (IOException e) {e.printStackTrace();}
				T.timepointN++;
				Info.out("Task"+T.id+"("+T.name+")"+" Save task data finished.");
				Info.out("Task"+T.id+"("+T.name+")"+" Synchronized.");
		}
	}
	public void limitCPU_usage(int requestFreq) {
		if(requestFreq<=0) return;
		do{try {Thread.sleep(requestFreq);} catch (InterruptedException e) {e.printStackTrace();}}while(!T.active);
		T.active=false;
	}
	public String mapToString(HashMap<String,String> Tmap) {
		StringBuffer resu = new StringBuffer();
		for (Map.Entry<String, String> entry : Tmap.entrySet()) {
			//if(T.delay>0) limitCPU_usage(T.delay/10*15);
			//System.out.print("*");
			resu.append(entry.getKey()); resu.append(Define.splitSig); resu.append(entry.getValue()); resu.append("\r\n");
		}
	    return resu.toString();
	}
	public void getNewFileMD(String pathName,int depth,HashMap<String,String> resu,int requestFreq) throws IOException, InterruptedException {
		File dirFile = new File(pathName);   
        if (!dirFile.exists()) return;
        if (!dirFile.isDirectory()) {
            if (dirFile.isFile()) {
            	File file = new File(pathName);
            	if(FileOperation.checkF_Occupied(pathName)) resu.put(PathUtil.getRightPath(T.pathFrom, pathName),Define.occupied);
            	else resu.put(PathUtil.getRightPath(T.pathFrom, pathName),FileOperation.getFileFingerprint(pathName, 0));
            	return ;
            }
            return ;  
        }
        limitCPU_usage(requestFreq);
        String[] fileList = dirFile.list();
        int currentDepth=depth+1;  
        for (int i = 0; i < fileList.length; i++) {
        	limitCPU_usage(requestFreq);
            String string = fileList[i];   
            File file = new File(dirFile.getPath(),string);  
            String path = file.getAbsolutePath();
            if (file.isDirectory()) getNewFileMD(file.getCanonicalPath(),currentDepth,resu,requestFreq);  
            else if(FileOperation.checkF_Occupied(path)) resu.put(PathUtil.getRightPath(T.pathFrom, path),Define.occupied);
            		else resu.put(PathUtil.getRightPath(T.pathFrom, path),FileOperation.getFileFingerprint(path, 0));
        }
        return;
	}
	public void freadFileMD(String path,HashMap<String,String> resu) throws IOException, InterruptedException {
		String tmp=FileOperation.readFile(path);
		String[] tmp0=tmp.split("\r?\n");
		for(int i=0;i<tmp0.length;i++) {
			String[] tmp1=tmp0[i].split(Define.splitSig);
			if(tmp1.length!=2){
				resu.clear();fixFileMD(resu); 
				Info.wrn("Task"+T.id+"("+T.name+")"+" FilesLastModifiedDate.txt were damaged, fixed it.");
				return;
			}
			resu.put(tmp1[0], tmp1[1]);
		}
	}
	public void fixFileMD(HashMap<String,String> resu) throws IOException, InterruptedException {
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
		TreeMap<String,String> Fmap=new TreeMap<String,String>();
		for(int i=0;i<T.timepointN;i++) {
			Timepoint tmptp=(Timepoint) timepoints.get(i);
			List Flist = new ArrayList<String>(); FileOperation.findFiles(T.pathTo+Define.sl+tmptp.path, 0, Flist);
			for(int j=0;j<Flist.size();j++) {
				String key=PathUtil.getRightPath(T.pathTo+Define.sl+tmptp.path, Flist.get(j).toString()), val=Flist.get(j).toString();
				Fmap.put(key, val);
			}
			String[] tmp1=FileOperation.readFile(T.pathTo+Define.sl+tmptp.path+Define.sl+Define.delListFileName).split("\r\n");
			for(int j=0;j<tmp1.length;j++) Fmap.remove(tmp1[j]);
		}
		Iterator<Entry<String, String>> it=Fmap.entrySet().iterator();
		while (it.hasNext()) {
		    Entry<String, String> E = it.next();
		    String tmp3=T.pathFrom+Define.sl+E.getKey();
		    if(E.getKey().equals(Define.sl+Define.delListFileName)) continue;
		    resu.put(E.getKey(), FileOperation.getFileFingerprint(tmp3, 0));
		}
	}
}
