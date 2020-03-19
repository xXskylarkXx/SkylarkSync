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
import java.util.Iterator;
import java.util.List;
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
	Task T=Share.curentTask;
	public boolean running,stop=false;
	@Override
	public void run() {
		Share.doneCpyTask=true;
		boolean firstload=true;
		Info.out("Task"+T.id+"("+T.name+")"+" loaded successfully");
		try {Thread.sleep(10000);} catch (InterruptedException e) {e.printStackTrace();}
		while(!stop) {
			/*Sleep*/
			if(!firstload) {
				T.FileMD_new.clear(); T.FileMD_old.clear(); System.gc();
				for(int i=0;i<(T.freq*60000)/1000;i++) {
					if(Share.pause) break;
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
			}else firstload=false;
			/*Main thread interaction*/
				running=false;
				while(Share.pause)try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				running=true;
			/*Synchronization*/
				/*Collect data*/
				boolean usedTimepointN=false;
				try {FileOperation.writeFile(T.pathTo+"\\"+Define.dataFolder+"\\tmp\\creator.txt", "");} catch (IOException e1) {e1.printStackTrace();}
				T.FileMD_new.clear();
				try {getNewFileMD(T.pathFrom,0,T.FileMD_new);} catch (IOException e1) {e1.printStackTrace();}
				String path_file_timepointN=T.pathTo+"\\"+Define.dataFolder+"\\TimePointN.txt";
				File file_timepointN=new File(path_file_timepointN);
				if(!file_timepointN.exists()) {
					try {FileOperation.writeFileTxt(path_file_timepointN, "0");} catch (IOException e) {e.printStackTrace();}
					Info.wrn("Task"+T.id+"("+T.name+")"+" TimePointN.txt doesn't exists, created a new one.");
				}
				try {T.timepointN=Integer.parseInt(FileOperation.readFile(path_file_timepointN));} catch (NumberFormatException | IOException e3) {e3.printStackTrace();}
				File file_FMD=new File(T.pathTo+"\\"+Define.dataFolder+"\\FilesLastModifiedDate.txt");
				if(!file_FMD.exists()) {
					try {FileOperation.writeFile(T.pathTo+"\\"+Define.dataFolder+"\\FilesLastModifiedDate.txt",mapToString(Share.thpool[T.id].T.FileMD_new.entrySet().iterator()));} catch (IOException e) {e.printStackTrace();}
					Info.wrn("Task"+T.id+"("+T.name+")"+" FilesLastModifiedDate.txt doesn't exists, created a new one.");
				}else try {freadFileMD(T.pathTo+"\\"+Define.dataFolder+"\\FilesLastModifiedDate.txt",T.FileMD_old);} catch (IOException e2) {e2.printStackTrace();}
				/*Compare files*/
				Iterator<Entry<String, String>> it_new=T.FileMD_new.entrySet().iterator(), it_old=T.FileMD_old.entrySet().iterator();
				String nowTime=TimeUtil.timeFormat.format(new Date());
				while (it_new.hasNext()) {
				    Entry<String, String> cur = it_new.next();
				    String curKey=cur.getKey(), curVal=cur.getValue();
				    String resu=T.FileMD_old.get(curKey);
				    String pathDest=T.pathTo+"\\"+T.timepointN+"__"+nowTime+"\\"+curKey;
				    File tmpF=new File(T.pathFrom+"\\"+curKey);
				    if(curVal.equals(Define.occupied)) {Info.wrn("Task"+T.id+"("+T.name+") "+tmpF.getName()+" were locked by other programme, skipped it."); continue;}
				    if(resu==null) {/*If this file doesn't exists*/
				    	FileOperation.cpyFile(T.pathFrom+"\\"+curKey,pathDest);
				    	usedTimepointN=true;
				    	continue;
				    }
				    if(!resu.equals(curVal)) {/*If this file were modified*/
				    	FileOperation.cpyFile(T.pathFrom+"\\"+curKey,pathDest);
				    	usedTimepointN=true;
				    	continue;
				    }
				}
				String delFileList="";
				while (it_old.hasNext()) {
				    Entry<String, String> cur = it_old.next();
				    String curKey=cur.getKey(), curVal=cur.getValue();
				    String resu=T.FileMD_new.get(curKey);
				    if(resu==null) {/*If this file were deleted*/
				    	delFileList+=(curKey+"\r\n");
				    	usedTimepointN=true;
				    	continue;
				    }
				}
				/*Synchronized ,saving data*/
				if(usedTimepointN) try {FileOperation.writeFileTxt(T.pathTo+"\\"+T.timepointN+"__"+nowTime+"\\"+Define.delListFileName, delFileList); usedTimepointN=true;} catch (IOException e1) {e1.printStackTrace();}
				try {FileOperation.writeFileTxt(T.pathTo+"\\"+Define.dataFolder+"\\FilesLastModifiedDate.txt",mapToString(Share.thpool[T.id].T.FileMD_new.entrySet().iterator()));} catch (IOException e) {e.printStackTrace();}
				if(usedTimepointN) T.timepointN++;
				try {FileOperation.writeFileTxt(path_file_timepointN, String.valueOf(T.timepointN));} catch (IOException e) {e.printStackTrace();}
		}
	}
	public static String mapToString(Iterator<Entry<String, String>> it) {
		String resu = "";
	    while (it.hasNext()) {
	      Entry<String, String> entry = it.next();
	      resu+=entry.getKey()+Define.splitSig+entry.getValue()+"\r\n";
	    }
	    return resu;
	}
	public void getNewFileMD(String pathName,int depth,TreeMap<String,String> resu) throws IOException {
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
        String[] fileList = dirFile.list();
        int currentDepth=depth+1;  
        for (int i = 0; i < fileList.length; i++) {
            String string = fileList[i];   
            File file = new File(dirFile.getPath(),string);  
            String path = file.getAbsolutePath();
            if (file.isDirectory()) getNewFileMD(file.getCanonicalPath(),currentDepth,resu);  
            else if(FileOperation.checkF_Occupied(path)) resu.put(PathUtil.getRightPath(T.pathFrom, path),Define.occupied);
            		else resu.put(PathUtil.getRightPath(T.pathFrom, path),FileOperation.getFileFingerprint(path, 0));
        }
        return;
	}
	public void freadFileMD(String path,TreeMap<String,String> resu) throws IOException {
		String tmp=FileOperation.readFile(path);
		String[] tmp0=tmp.split("\\r?\\n");
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
	public void fixFileMD(TreeMap<String,String> resu) throws IOException {
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
			List Flist = new ArrayList<String>(); FileOperation.findFiles(T.pathTo+"\\"+tmptp.path, 0, Flist);
			for(int j=0;j<Flist.size();j++) {
				String key=PathUtil.getRightPath(T.pathTo+"\\"+tmptp.path, Flist.get(j).toString()), val=Flist.get(j).toString();
				Fmap.put(key, val);
			}
			String[] tmp1=FileOperation.readFile(T.pathTo+"\\"+tmptp.path+"\\"+Define.delListFileName).split("\\r\\n");
			for(int j=0;j<tmp1.length;j++) Fmap.remove(tmp1[j]);
		}
		Iterator<Entry<String, String>> it=Fmap.entrySet().iterator();
		while (it.hasNext()) {
		    Entry<String, String> E = it.next();
		    String tmp3=T.pathFrom+"\\"+E.getKey();
		    if(E.getKey().equals("\\"+Define.delListFileName)) continue;
		    resu.put(E.getKey(), FileOperation.getFileFingerprint(tmp3, 0));
		}
	}
}
