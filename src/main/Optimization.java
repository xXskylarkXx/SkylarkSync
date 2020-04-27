package main;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;

import share.Define;
import share.Share;
import share.Timepoint;
import util.FileOperation;
import util.Info;
import util.PathUtil;

public class Optimization {
	public static int optimize(int taskid) throws ParseException, IOException, InterruptedException {
		//judge
		List<Timepoint> timepoints=Main.getTimePointList(taskid);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		/*boolean cnt=false;
		for(int i=0;i<timepoints.size();i++) {
			Date cur=sdf.parse(timepoints.get(i).time), now=sdf.parse(sdf.format(System.currentTimeMillis()));
			long timegap_now=now.getTime()-cur.getTime();
			if(timegap_now<=Define.nd*10 && timegap_now>Define.nd) {cnt=true; break;}
			if(timegap_now<=Define.nd*30 && timegap_now>Define.nd*10) {cnt=true; break;}
			if(timegap_now>Define.nd*30) {cnt=true; break;}
		}
		if(cnt==true) {
			Info.out("It's too early to optimize within 4 days.");
			return -1;
		}
		//optimize*/
		boolean repeat; int delN=0;
	    do{
	    	repeat=false; timepoints=Main.getTimePointList(taskid); 
	    	for(int i=timepoints.size()-1;i>0;i--) {
		    	Date newer=sdf.parse(timepoints.get(i).time), older=sdf.parse(timepoints.get(i-1).time), now=sdf.parse(sdf.format(System.currentTimeMillis()));
		    	long timegap_now=now.getTime()-newer.getTime(), timegap_interval=newer.getTime()-older.getTime();
		    	if(timegap_now<=Define.nd) {
		    		if(timegap_interval<Define.nh*6) {
		    			mergeTimePoint(taskid,i-1,i);
		    			repeat=true; delN++;
		    			break;
		    		}
		    	}
		    	if(timegap_now<=Define.nd*10 && timegap_now>Define.nd) {
		    		if(timegap_interval<Define.nd*3) {
		    			mergeTimePoint(taskid,i-1,i);
		    			repeat=true; delN++;
		    			break;
		    		}
		    	}
		    	if(timegap_now<=Define.nd*30 && timegap_now>Define.nd*10) {
		    		if(timegap_interval<Define.nd*10) {
		    			mergeTimePoint(taskid,i-1,i);
		    			repeat=true; delN++;
		    			break;
		    		}
		    	}
		    	if(timegap_now>Define.nd*30) {
		    		if(timegap_interval<Define.nd*60) {
		    			mergeTimePoint(taskid,i-1,i);
		    			repeat=true; delN++;
		    			break;
		    		}
		    	}
	    	}
	    }while(repeat);
		return delN;
	}
	public static void mergeTimePoint(int taskid,int from,int to) throws IOException, InterruptedException {
		/*merge*/
		Task T=Share.thpool[taskid].T;
		List<Timepoint> timepoints=Main.getTimePointList(taskid);
		TreeMap<String,String> Fmap=new TreeMap<String,String>();
		String sumDelList = "";
		for(int i=from;i<=to;i++) {
			Timepoint tmptp=timepoints.get(i);
			List Flist = new ArrayList<String>(); FileOperation.findFiles(T.pathTo+Define.sl+tmptp.path, 0, Flist);
			for(int j=0;j<Flist.size();j++) {
				String key=PathUtil.getRightPath(T.pathTo+Define.sl+tmptp.path, Flist.get(j).toString()), val=Flist.get(j).toString();
				Fmap.put(key, val);
			}
			sumDelList+=FileOperation.readFile(T.pathTo+Define.sl+tmptp.path+Define.sl+Define.delListFileName);
		}
		FileOperation.writeFileTxt(T.pathTo+Define.sl+timepoints.get(to).path+Define.sl+Define.delListFileName, sumDelList);
		List<String> delList = new ArrayList();
		for (Map.Entry<String, String> entry : Fmap.entrySet())
			if(entry.getValue().contains(timepoints.get(to).path) || entry.getValue().contains(Define.delListFileName)) delList.add(entry.getKey());
		for(int i=0;i<delList.size();i++) Fmap.remove(delList.get(i));
		for (Map.Entry<String, String> entry : Fmap.entrySet())
			FileOperation.moveFile(entry.getValue() ,T.pathTo+Define.sl+timepoints.get(to).path+Define.sl+entry.getKey());
		for(int i=from;i<to;i++) {
			File delF=new File(T.pathTo+Define.sl+timepoints.get(i).path);
			FileUtils.deleteDirectory(delF);
		}
		/*sort*/
		timepoints=Main.getTimePointList(taskid);
		for(int i=0;i<timepoints.size();i++) {
			Timepoint Ttp=timepoints.get(i);
			String[] tmp=Ttp.path.split("__");
			if(!tmp[0].equals(String.valueOf(i))) {
				File Ffrom=new File(T.pathTo+Define.sl+Ttp.path), Fto=new File(T.pathTo+Define.sl+i+"__"+tmp[1]);
				Ffrom.renameTo(Fto);
			}
		}
		String path_file_timepointN=T.pathTo+Define.sl+Define.dataFolder+Define.sl+"TimePointN.txt";
		try {FileOperation.writeFileTxt(path_file_timepointN, String.valueOf(timepoints.size()));} catch (IOException e) {e.printStackTrace();}
	}
}
