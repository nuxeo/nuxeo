package com.anwrt.ooserver.daemon;

/**
 * Static class.<br>
 * The logger is used by calling Logger's static methods such as info, debug, etc...<br><br>
 * <strong>WARNING</strong> You must first instantiate a Logger,<br>
 * Logger.newInstance(new LoggerBasicImpl()); for example. <br>
 * Other loggers can be created using LoggerBasicImpl as an example.<br>
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public abstract class Logger
{
	public static final int 	  	NONE 			= 0;
	public static final int		  	ERROR			= 1;
	public static final int 	  	INFO			= 2;
	public static final int			WARNING			= 3;
	public static final int 	  	DEBUG			= 4;
	public static final int			DETAILED_DEBUG 	= 5;
	public static final int			NB_LEVELS		= 6;
	public static final String[] 	levelStrings = {"NONE", "ERROR", "INFO", "WARNING",
												"DEBUG", "DETAILED_DEBUG"};
	
	protected static int 			level 			= DETAILED_DEBUG;
	
	private static Logger		_instance = null;
	
	/**
	 * Used to specify which instance will be used by the static methods
	 * of the Logger.
	 * @param instance a child of the abstract class Logger (ex : LoggerBasicImpl)
	 */
	public static void newInstance(Logger instance)
	{
		_instance = instance;
	}
	/**
	 * Returns the current Logger instance.
	 * @return current instance
	 */
	public static Logger getInstance()		{ return _instance; }
	
	/**
	 * Used to set the log level.
	 * An logger instance is not needed, all this is static.
	 * @param lvl level such as INFO, WARNING, DEBUG, ...
	 * @throws IncorrectLoggerLevelException level passed as an argument is not valid
	 */
	public static void setLevel(int lvl) throws IncorrectLoggerLevelException
	{
		if (lvl >= NB_LEVELS || lvl < 0) 
		{
			level = NB_LEVELS - 1; // higher level
			throw new IncorrectLoggerLevelException("" + lvl);
		}
		else level = lvl;
	}
	/**
	 * Used to set the log level.
	 * An logger instance is not needed, all this is static.
	 * @param lvlStr level such as INFO, WARNING, DEBUG, ...
	 * @throws IncorrectLoggerLevelException level passed as an argument is not valid
	 */
	public static void setLevel(String lvlStr) throws IncorrectLoggerLevelException
	{
		String lvl = lvlStr.toUpperCase();
		if (lvl.equals("NONE"))
			level = NONE;
		else if (lvl.equals("INFO"))
			level = INFO;
		else if (lvl.equals("WARNING"))
			level = WARNING;
		else if (lvl.equals("ERROR"))
			level = ERROR;
		else if (lvl.equals("DEBUG"))
			level = DEBUG;
		else if (lvl.equals("DETAILED_DEBUG"))
			level = DETAILED_DEBUG;
		else
		{
			level = NB_LEVELS - 1; // higher level
			throw new IncorrectLoggerLevelException(lvl);
		}
	}
	/**
	 * Converts the current level to String
	 * @return level as a string
	 */
	public String levelToString()
	{
		return levelStrings[level];
	}
	
	/**
	 * Returns log level
	 * @return log level
	 */
	public static int getLevel() { return level; }
	
	protected abstract void infoImpl(String msg);
	public static void info(String msg)
	{
		if (getInstance() == null) return;
		if (level >= INFO)
			getInstance().infoImpl(msg);
	}
	protected abstract void warningImpl(String msg);
	public static void warning(String msg)
	{
		if (getInstance() == null) return;
		if (level >= WARNING)
			getInstance().warningImpl(msg);
	}
	protected abstract void debugImpl(String msg);
	public static void debug(String msg)
	{
		if (getInstance() == null) return;
		if (level >= DEBUG)
			getInstance().debugImpl(msg);
	}
	protected abstract void debugImpl(String msg, Exception ex);
	public static void debug(String msg, Exception ex)
	{
		if (getInstance() == null) return;
		if (level >= DEBUG)
			getInstance().debugImpl(msg, ex);
	}
	protected abstract void debugImpl(Exception ex);
	public static void debug(Exception ex)
	{
		if (getInstance() == null) return;
		if (level >= DEBUG)
			getInstance().debugImpl(ex);
	}
	protected abstract void detailedDebugImpl(Exception ex);
	public static void detailedDebug(Exception ex)
	{
		if (getInstance() == null) return;
		if (level >= DETAILED_DEBUG)
			getInstance().detailedDebugImpl(ex);
	}
	protected abstract void errorImpl(String msg);
	public static void error(String msg)
	{
		if (getInstance() == null) return;
		if (level >= ERROR)
			getInstance().errorImpl(msg);
	}
	protected abstract void fatalErrorImpl(String msg);
	public static void fatalError(String msg)
	{
		if (getInstance() == null) return;
		if (level >= ERROR)
			getInstance().fatalErrorImpl(msg);
	}
	protected abstract void fatalErrorImpl(String msg, Exception ex);
	public static void fatalError(String msg, Exception ex)
	{
		if (getInstance() == null) return;
		if (level >= ERROR)
			getInstance().fatalErrorImpl(msg, ex);
	}
	
	/**
	 * displays logger's level
	 * @see java.lang.Object#toString()
	 * @return -
	 */
	public String toString()
	{
		return "Logger[level=" + levelToString() + "]";
	}
	
	/**
	 * Returns informations about logging levels
	 * @param prefixTabs optional tabs that will be written before displaying 
	 * a line for indentation
	 * @return a string containing a list of levels with their description
	 */
	public static String levelInformation(String prefixTabs)
	{
		// !!! Modifications must be done to the README too
		if (prefixTabs == null) prefixTabs = "";
		String ret = 
			  prefixTabs + "NONE           : " + "no output, nothing, nada\n"
			+ prefixTabs + "ERROR          : " + "only errors and fatal errors will be displayed \n" 
			+ prefixTabs + "                 " + "(stack traces not available)\n"
			+ prefixTabs + "INFO           : " + "informations about the execution process will be displayed\n"
			+ prefixTabs + "WARNING        : " + "warnings displayed added to previous level informations\n"
			+ prefixTabs + "DEBUG          : " + "debug information ( + exceptions stack traces )\n"
			+ prefixTabs + "DETAILED_DEBUG : " + "debug + minor informations \n "
			+ prefixTabs + "                 " + "(exceptions during tries, sleep, etc...)\n";
		return ret;
	}
}
