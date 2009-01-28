package com.anwrt.ooserver.daemon;

/**
 * 
 * Basic Logger implementation that throw everything to standard outputs.<br>
 * Use it as an example if you want to make something more inventive.
 * <br>
 * creation : 28 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class LoggerBasicImpl extends Logger
{
	protected static final String     	TAG_INFO     	= "[ info  ]";
	protected static final String 		TAG_WARNING		= "[ warn  ]";
	protected static final String     	TAG_ERROR    	= "[ ERROR ]";
	protected static final String     	TAG_FATAL   	= "[ FATAL ]";
	protected static final String     	TAG_DEBUG    	= "[ debug ]";
	
	protected static String getMessageString(String tag, String msg)
	{
	    return tag + " " + msg;
	}
	protected void infoImpl(String msg)
	{
		System.out.println(getMessageString(TAG_INFO, msg));
	}
	protected void warningImpl(String msg)
	{
		if (level >= WARNING)
			System.out.println(getMessageString(TAG_WARNING, msg));
	}
	protected void debugImpl(String msg)
	{
		System.out.println(getMessageString(TAG_DEBUG, msg));
	}
	protected void debugImpl(String msg, Exception ex)
	{
		debugImpl(msg);
		debugImpl(ex);
	}
	protected void debugImpl(Exception ex)
	{
		exception(ex);
	}
	protected void detailedDebugImpl(Exception ex)
	{
		exception(ex);
	}
	protected void errorImpl(String msg)
	{
		System.err.println(getMessageString(TAG_ERROR, msg));
	}
	protected void fatalErrorImpl(String msg)
	{
		System.err.println(getMessageString(TAG_FATAL, msg));
	}
	protected void fatalErrorImpl(String msg, Exception ex)
	{
		System.err.println(getMessageString(TAG_FATAL, msg));
		exception(ex);
	}
	protected static void exception(Exception ex)
	{
		ex.printStackTrace();
	}
}
