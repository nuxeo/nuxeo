//------------------------------------------------
// $Id: IncorrectLoggerLevelException.java,v 1.2 2007/08/28 15:27:08 jounayd Exp $
// (c) Anyware Technologies 2007    www.anyware-tech.com
//------------------------------------------------
package com.anwrt.ooserver.daemon;

/**
 * 
 * The string that seems to represent a level is not a valid level (syntax error,
 * try buying glasses or putting 'INFO', 'DEBUG', ...)
 * <br>
 * creation : 28 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class IncorrectLoggerLevelException extends Exception
{

	static final long serialVersionUID = -4326994778066442457L;
	
	public IncorrectLoggerLevelException(String lvl)
	{
		super("Bad logger level : " + lvl);
	}
}
