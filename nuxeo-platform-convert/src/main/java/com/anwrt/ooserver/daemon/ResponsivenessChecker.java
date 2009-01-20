package com.anwrt.ooserver.daemon;

import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * 
 * Thread that tests if an OpenOffice process is still available and responsive
 * (no deadlock, etc...)
 * <br>
 * creation : 30 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class ResponsivenessChecker extends Thread
{
	private OfficeProcess	_process;
	private boolean 		_responsive;
	private Config			_config;
	
	private synchronized boolean getResponsive()
	{
		return _responsive;
	}
	public ResponsivenessChecker(Config config, OfficeProcess process)
	{
		super();
		_process 	= process;
		_responsive	= false;
		_config 	= config;
	}
	/**
	 * Tests if it's responsive in a certain amount of time, unless, it's not
	 * @return true if responsive
	 */
	public boolean isResponsive()
	{
		try
		{
			this.join(_config.sleepingDelay);
		}
		catch (InterruptedException ex)
		{
		    Logger.debug(ex);
		}
		return getResponsive();
	}
	/**
	 * Ask to the office process some random things and if 
	 * there is an answer, _responsive is set to true
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try
		{
			synchronized (this)
			{
				// Still alive ?
				XComponentContext context		= _process.getContext();
				XMultiComponentFactory manager 	= context.getServiceManager();
				Object desktopObj 				= manager.
					createInstanceWithContext("com.sun.star.frame.Desktop", context);
				XDesktop desktop				= (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktopObj);
				
				// Check for typical solar-mutex deadlock
				desktop.getCurrentComponent();
				
				// more checks may be added
				_responsive = true;
			}
		}
		catch (Exception ex)
		{
			Logger.error("responsiveness-check for " + _process 
			        + " failed: " + ex);
			Logger.debug(ex);
			_responsive = false;
		}
	}
}
/*

*/