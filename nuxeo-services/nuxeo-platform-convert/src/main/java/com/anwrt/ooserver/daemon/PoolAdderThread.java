package com.anwrt.ooserver.daemon;

/**
 * 
 * Make an office process reenter the ready pool
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class PoolAdderThread extends Thread
{
	private OfficeProcess 		_process 		= null;
	private ProcessPool			_processPool 	= null;

	public PoolAdderThread(ProcessPool processPool, OfficeProcess process)
	{
		super();
		_processPool 	= processPool;
		_process 		= process;
	}
	/**
	 * Restarts the process if necessary,
	 * then add it to the ready pool
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try
		{
			if (!_process.restartWhenNecessary())
			{
				Logger.fatalError("could not restart worker " + _process);
			}
			_processPool.append(_process);
			Logger.info(_processPool.getStateString() + " <- " + _process + " reenters pool");
		}
		catch (Exception ex)
		{
			Logger.fatalError("cannot add an openOffice instance into pool");
			Logger.debug(ex);
		}
	}
	
}
