package com.anwrt.ooserver.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * An abstract layer that is useful for some system calls
 * <br>
 * creation : 30 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class OSAbstractLayer 
{
	public static final int 	OS_UNKNOWN			= 0;
	public static final int 	OS_WINDOWS			= 1;
	public static final int 	OS_UNIXCOMPATIBLE	= 2;
	
	private int 		_os = OS_UNKNOWN;
	private Runtime		_runtime;
	
	public OSAbstractLayer () throws Exception
	{
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("windows"))
			_os = OS_WINDOWS;
		/*else if (osName.startsWith("SunOS") || osName.startsWith("Linux"))
			os = OS_UNIXCOMPATIBLE;*/
		else
		{
			_os = OS_UNIXCOMPATIBLE;
			//throw new Exception("Unknown OS, killing impossible");
		}
		_runtime = Runtime.getRuntime();
	}
	/**
	 * 
	 * @param processName
	 * @throws IOException
	 */
	public void kill(String processName) throws IOException
	{
		switch(_os)
		{
		case OS_WINDOWS:
			String[] cmd = {"tskill", processName};
			Runtime.getRuntime().exec(cmd);
			break;
		case OS_UNIXCOMPATIBLE:
			Runtime runtime = Runtime.getRuntime();

		  	String pid = getProcessID(processName);
		  	if (pid != null)
		  	{
		  		while (pid != null)
		  		{
		  			String[] killCmd = {"/bin/bash", "-c", "kill -9 " + pid};
		  			runtime.exec(killCmd);
		
		  			// Is another process running?
		  			pid = getProcessID(processName);
		  		}
		  	}
			break;

		default:
			break;
		}
	}
	/**
	 * Only working for UnixCompatible systems
	 * @param processName the exact name of the process
	 * @return
	 * @throws IOException
	 */
	private String getProcessID(String processName) throws IOException
    {
		switch(_os)
		{
		case OS_UNIXCOMPATIBLE:
			Runtime runtime = Runtime.getRuntime();

			// Get process id
			String[] getPidCmd = {"/bin/bash", "-c", "ps -e|grep " + processName + "|awk '{print $1}'"};
			Process getPidProcess = runtime.exec(getPidCmd);

			// Read process id
			InputStreamReader isr = new InputStreamReader(getPidProcess.getInputStream());
			BufferedReader br = new BufferedReader(isr);

			return br.readLine();
		default:
			return "";
		}
    }
	/**
	 * not working
	 * @param pid
	 */
	public boolean isProcessAlive(int pid)
	{
		String nullDevice = "0";
		// Get process
		String[] cmd = {"ps", "-p", "" + pid, ">", nullDevice};
		//Process cmdProcess = runtime.exec(cmd);
		
		// Read process
		//cmdProcess.exitValue() 
		//return runtime.exec( cmd );
		return true;
	}
	/**
	 * example : 
	 *   path1 = "C:/Program Files"
	 *   path2 = "directory"
	 *   result = "C:/Program Files/directory"
	 *   
	 * separators are '/' or '\'
	 * @param path1 a string ended by a separator or not
	 * @param path2 any string (should not start by a separator)
	 * @return The result of the concatenation of path1 and path2
	 */
	public static String concatPaths(String path1, String path2)
	{
		String ret;
		char lastChar = path1.charAt(path1.length() - 1);
		if (lastChar == '/' || lastChar == '\\')
		{
			ret = path1 + path2;
		}
		else
		{
			ret = path1 + java.io.File.separator + path2;
		}
		return ret;
	}
}