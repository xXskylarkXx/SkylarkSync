package util;

public class PathUtil {
	public static String getRightPath(String parentPath,String childPath) {return childPath.replace(parentPath, "");}
}
