package main;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class Task{
	public boolean active;
	public String name, pathFrom, pathTo;
	public int id, freq, timepointN, delay;
	public HashMap<String,String> FileMD_old, FileMD_new;
}