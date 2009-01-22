package com.anwrt.ooserver.daemon;

import com.sun.star.bridge.XInstanceProvider;
import com.sun.star.lib.uno.helper.ComponentBase;

/**
 * 
 * Returns the open office instance from the bridge
 * <br>
 * creation : 30 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class OfficeInstanceProvider extends ComponentBase implements XInstanceProvider
{
	private OfficeProcess 		_process;
	
	public OfficeInstanceProvider(OfficeProcess process)
	{
		_process 	= process;
	}

	/**
	 * Returns the office instance object found by a name
	 * @see com.sun.star.bridge.XInstanceProvider#getInstance(java.lang.String)
	 */
	public Object getInstance(String name)
	{
		Logger.debug("resolving name " + name);
		Object object = _process.getBridge().getInstance(name);
		return object;
	}
}