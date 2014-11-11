package com.anwrt.ooserver.daemon;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * Pool of ready Open Office server instances
 * <br>
 * creation : 30 ao�t 07
 *
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class ProcessPool
{
    private LinkedList		_list 		= null;
    private OfficeProcess[]	_all		= null;
    private Config 			_config 	= null;

    /**
     * Returns an array of all the office instances available,
     * busy or not.
     * @return process array
     */
    public OfficeProcess[] getAll() { return _all; }

    /**
     * Just keeps some configuration for further initialization
     * @param config any configuration
     */
    public ProcessPool(Config config)
    {
        _list 		= new LinkedList();
        _config 	= config;
    }
    /**
     * append an office process into the ready pool, at the end of the queue
     * @param item the office process
     */
    public synchronized void append(OfficeProcess item)
    {
        if (item == null)
        {
            Logger.error("Appending null OfficeProcess to the pool");
            return;
        }
        _list.add(item);
        notify();
    }
    /**
     * Initialization finished, we keep a reference to all processes in _all
     */
    public void initializationFinished()
    {
        Object[] array = _list.toArray();
        _all = new OfficeProcess[array.length];
        for (int i = 0; i < array.length; i++)
            _all[i] = (OfficeProcess) array[i];
    }
    /**
     * Number of ready processes
     * @return -
     */
    public int size()
    {
        return _list.size();
    }

    /**
     * Ask all office process (ready or not) to terminate
     */
    public void terminate()
    {
        for (int i = 0; i < _all.length; i++)
            _all[i].terminate();
    }
    /**
     * Gets the first ready process that entered the pool
     * (also checks if it's responsive)
     * @return a ready office process
     */
    public synchronized OfficeProcess pop()
    {
        OfficeProcess ret = null;
        while (ret == null)
        {
            if (_list.size() != 0)
                ret = (OfficeProcess) _list.removeFirst();
            else
            {
                // waiting for a process to reenter pool
                try
                {
                    wait(_config.sleepingDelay);
                }
                catch (InterruptedException ex)
                {
                    Logger.detailedDebug(ex);
                }
            }
            // Test if the process is responsive
            if (ret != null && !ret.isResponsive())
            {
                Logger.info("OfficeProcess died, restarting a new one");
                new PoolAdderThread(this, ret).start();
                notify();
                ret = null;
            }
        }
        return ret;
    }
    /**
     * Wait until the process is ready and connected
     */
    public void waitTillReady()
    {
        Iterator it = _list.iterator();
        while (it.hasNext())
        {
            OfficeProcess p 		= (OfficeProcess) it.next();
            int 	t 				= size() * _config.toleratedStartupTimePerInstance;
            if (!p.waitTillReady(new Integer(t)))
            {
                Logger.fatalError("process not ready : " + p);
                return;
            }
        }
    }
    /**
     * returns "ready processes count / processes count"
     * @return an information string
     */
    public String getStateString()
    {
        return "{" + size() + "/" + _config.userInstallation.size() + "}";
    }
}
