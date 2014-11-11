package com.anwrt.ooserver.daemon;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Vector;
import com.sun.star.uno.Type;
import com.sun.star.beans.NamedValue;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.container.XNameAccess;

/**
 * 
 * Kind of interface structure to exchange status informations between the main
 * daemon process and an admin daemon process.
 * <br>
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class Status extends ComponentBase implements XNameAccess
{
	private HashMap 		_map;
	private int 			_available;
	private Vector			_workers;

	/**
	 * Creates a structure that represents a fixed office process list
	 * @param processList the fixed process list
	 */
	public Status(OfficeProcess[] processList)
	{
		_map 			= new HashMap();
		_map			.put("poolsize", new Integer(processList.length));
		_available 		= 0;
		_workers		= new Vector();

		for (int j = 0; j < processList.length; j++)
		{
			OfficeProcess p = processList[j];
			NamedValue 	v 	= null;
			if (p.getTimestamp() == null)
			{
				v = new NamedValue("usage-time", new Long(0));
				_available++;
			}
			else
			{
				v = new NamedValue("usage-time",
						new Long(System.currentTimeMillis() - p.getTimestamp().longValue()));
			}
			NamedValue[] t = 
				{new NamedValue("usage", p.getUsage()),
				v,
				new NamedValue("user-dir", p.getUserId()),
				new NamedValue("index", p.getIndex()),
				};
			_workers.add(t);
		}
		_map.put("workers", _workers.toArray());
		_map.put("available", new Integer(_available));
	}
	public Object getByName(String name) throws NoSuchElementException
	{
		if (_map.containsKey(name))
		{
			return _map.get(name);
		}
		throw new NoSuchElementException("Unknown element " + name);
	}
	public String[] getElementNames()
	{
		return (String[]) _map.keySet().toArray();
	}
	public boolean hasByName(String name)
	{
		return _map.containsKey(name);
	}
	public boolean hasElements()
	{
		return true;
	}
	public Type getElementType()
	{
		return new Type();
	}
}
