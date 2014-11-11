package com.anwrt.ooserver.daemon;

import com.sun.star.frame.XDesktop;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * 
 * Thread that tries to shutdown one open office process.
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class TerminateThread extends Thread
{
	private XComponentContext 	_context;
	
	/**
	 * Just keep the context
	 * @param context current context to use
	 */
	public TerminateThread(XComponentContext context)
	{
		super();
		_context 	= context;
	}
	/**
	 * Send a terminate message to the office process
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try
		{
			Object desktopObj 			= _context.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", _context);
			XDesktop desktop			= (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktopObj);
			desktop						.terminate();
		}
		catch (Exception ex)
		{
			Logger.debug("TerminateThread exception : ", ex);
		}
	}
}
