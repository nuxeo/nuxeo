package com.anwrt.ooserver.daemon;

import com.sun.star.bridge.XInstanceProvider;
import com.sun.star.lib.uno.helper.ComponentBase;

/**
 * 
 * Empty instance, not used for normal situations
 * <br>
 * creation : 28 août 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class EmptyPoolInstanceProvider extends ComponentBase implements XInstanceProvider
{
	public Object getInstance(String name)
	{
		return null;
	}
}