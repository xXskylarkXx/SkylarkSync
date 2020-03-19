package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;

import share.Define;

public class FileOperation {
	static String encoding = "UTF-8";  
	public static String readFile(String fileName) throws IOException{ 
        File file = new File(fileName);  
        Long filelength = file.length();  
        byte[] filecontent = new byte[filelength.intValue()];  
            FileInputStream in=null;
			try {in = new FileInputStream(file);} catch (FileNotFoundException e) {e.printStackTrace();}  
            in.read(filecontent);  
            in.close();
            return new String(filecontent, encoding);  
    }
	public static void cpyFile(String from,String to){
		//Info.out(to);
		byte[] buf = new byte[1024*8]; int buflen;
		if(from.equals("") || to.equals("")) return;
	    File fileto = new File(to),filefrom = new File(from);
	    if(!fileto.getParentFile().exists()) fileto.getParentFile().mkdirs();
	    try {fileto.createNewFile();} catch (IOException e) {e.printStackTrace();}
	    FileInputStream in = null; FileOutputStream out = null;
		try {in = new FileInputStream(filefrom);} catch (FileNotFoundException e) {return;}
		try {out = new FileOutputStream(fileto);} catch (FileNotFoundException e) {e.printStackTrace();}
	    try {while((buflen=in.read(buf))!=-1) out.write(buf,0,buflen);} catch (IOException e) {e.printStackTrace();}
	    try {in.close();} catch (IOException e) {e.printStackTrace();}   try {out.close();} catch (IOException e) {e.printStackTrace();}
	}
	public static void writeFile(String fileName,String content) throws IOException {
		if(fileName.equals("")) return;
	    File file = new File(fileName);
	    if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
	    file.createNewFile();
	    FileOutputStream out=new FileOutputStream(file);
	    out.write(content.getBytes());
	    out.close();
	}
	public static void writeFileTxt(String fileName,String content) throws IOException {
		if(fileName.equals("")) return;
	    File file = new File(fileName);
	    if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
	    file.createNewFile();
	    OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
	    out.append(content);
	    out.close();
	}
	public static void findFiles(String pathName,int depth,List resu) throws IOException{
        File dirFile = new File(pathName);   
        if (!dirFile.exists()) return;
        if (!dirFile.isDirectory()) {
            if (dirFile.isFile()) {resu.add(pathName); return ;}
            return ;  
        }
        String[] fileList = dirFile.list();
        int currentDepth=depth+1;  
        for (int i = 0; i < fileList.length; i++) {
            String string = fileList[i];   
            File file = new File(dirFile.getPath(),string);  
            String path = file.getAbsolutePath();
            if (file.isDirectory()) findFiles(file.getCanonicalPath(),currentDepth,resu);  
            else resu.add(path);
        }
	}
	public static void cpyFolder(String from,String to) throws IOException{
        File Ffrom = new File(from), Fto= new File(to);   
        if(!Fto.exists()) Fto.mkdir();
        File[] tmpfiles=Ffrom.listFiles();
        for (File file : tmpfiles)
        	if (file.isFile()) cpyFile(from+"\\"+file.getName(), to+"\\"+file.getName());
            else cpyFolder(from+"\\"+file.getName(), to+"\\"+file.getName());
	}
	public static void delFile(String path) {File F=new File(path); F.delete();}
	public static void creatAllFolder(String path) {
		File F=new File(path);
		if(!F.getParentFile().exists()) creatAllFolder(F.getParentFile().getAbsolutePath());
		F.mkdir();
	}
	public static void cmd_win(String command){
		Process proc = null;
        boolean resu = false;
        try {proc=Runtime.getRuntime().exec("cmd.exe /c "+command+" & exit");} catch (IOException e1) {e1.printStackTrace();}
        InputStream in=proc.getInputStream();
        try {while(in.read()!=-1) {}} catch (IOException e) {e.printStackTrace();}
        try {in.close();} catch (IOException e) {e.printStackTrace();}
        try {proc.waitFor();} catch (InterruptedException e) {e.printStackTrace();}
    }
	public static boolean checkF_Occupied(String path) {
		File F=new File(path);
		FileInputStream in = null;
		try {in = new FileInputStream(F);} catch (FileNotFoundException e1) {return true;}
		try {in.read(); in.close();}catch (IOException e) {return true;}
		return false;
	}
	public static String getFileFingerprint(String path, int type) throws IOException {
		File Ftmp=new File(path);
		if(type==0) return String.valueOf(Ftmp.lastModified());
		if(type==1) {
			FileInputStream tmpfin=new FileInputStream(path);
			String md5=DigestUtils.md5Hex(tmpfin);
			tmpfin.close();
			return md5;
		}
		return "null type";
	}
}
