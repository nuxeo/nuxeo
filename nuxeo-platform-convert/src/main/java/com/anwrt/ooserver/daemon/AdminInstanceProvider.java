package com.anwrt.ooserver.daemon;

import java.util.Timer;
import java.util.TimerTask;
import com.sun.star.bridge.XInstanceProvider;
import com.sun.star.lib.uno.helper.ComponentBase;

/**
 * 
 * Used to execute admin actions and to return anything that the asker 
 * might want (status for example)
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class AdminInstanceProvider extends ComponentBase implements XInstanceProvider
{
	private 	Timer 			_shutdownTask;
	private 	Daemon			_daemon;
	
	public Daemon getDaemon() { return _daemon; }
	
	public AdminInstanceProvider(Daemon daemon, Timer shutdownTask)
	{
		_shutdownTask 	= shutdownTask;
		_daemon			= daemon;
	}
	/**
	 * Delayed shutdown
	 * <br>
	 *
	 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
	 */
	private class ShutdownTask extends TimerTask
	{
		public ShutdownTask() { /*DO NOTHING*/ }
		public void run()
		{
			getDaemon().shutdown();
		}
	}
	public Object getInstance(String name)
	{
		Logger.debug(AdminAcceptorThread.ADMIN_LOGGER_TAG + "requested command : " + name);
		Object object = null;
		if (name.equals("daemon.stop"))
		{
			_shutdownTask = new Timer();
			_shutdownTask.schedule(new ShutdownTask(), _daemon.getConfig().shutdownDelay);
		}
		else if (name.equals("daemon.status"))
			object = new Status(_daemon.getPool().getAll());
		else 
			Logger.error("AdminInstanceProvider: unknown command : " + name);
		
		return object;
	}
}
