package com.anwrt.ooserver.daemon;

import com.sun.star.io.XStreamListener;
import com.sun.star.lib.uno.helper.ComponentBase;

/**
 * 
 * Waits for a connection to stop. When the connection is lost or finished, the 
 * OfficeProcess corresponding is notified and added back to the ready process pool.
 * <br>
 * creation : 28 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class ConnectionListener extends ComponentBase implements XStreamListener
{
	private OfficeProcess 		_officeProcess = null; 
	private String				_conDesc;
	private ProcessPool			_processPool;

	public ConnectionListener(ProcessPool processPool, OfficeProcess officeProcess, String conDesc)
	{
		_processPool		= processPool;
		_officeProcess		= officeProcess;
		_conDesc			= conDesc;
	}
	/**
	 * End usage and add back to the ready process pool
	 */
	public void clear()
	{
		if (_officeProcess != null)
		{
			Logger.info(_conDesc + " disconnects from " 
			        + _officeProcess + " (used for  "
                   + Math.round(_officeProcess.getUsageDuration() * 0.001) + "s) ");
			_officeProcess.endUsage();
			new PoolAdderThread(_processPool, _officeProcess).start();
			_officeProcess = null;
		}
	}
	public void started() { /*DO NOTHING*/ }
	public void closed()
	{
		clear();
	}
	public void terminated()
	{
		clear();
	}
	public void error(Object obj)
	{
		clear();
	}
	/**
	 * This is a callback method used to inform that the remote bridge has gone down
	 *  Receives a notification about the connection has been closed.
	 *  @param source ...
	 */
	public void disposing(com.sun.star.lang.EventObject source)
	{
	    /*DO NOTHING*/
	}
}