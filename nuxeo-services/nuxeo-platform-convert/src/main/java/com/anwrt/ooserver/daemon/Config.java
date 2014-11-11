package com.anwrt.ooserver.daemon;

import java.util.ArrayList;
import java.io.File;

/**
 *
 * A container that have all informations about configuration
 * <br>
 *
 * @see com.anwrt.ooserver.daemon.ConfigHandler
 * @author <a href="mailto:oodaemon@extraserv.net">Jounayd Id Salah</a>
 */
public class Config
{
    public String 		adminAcceptor						= null;
    public String 		acceptor 							= null;
    public String 		officeProgramDirectoryPath	        = "./program/";
    public ArrayList	userInstallation 					= new ArrayList();
    public int			toleratedStartupTimePerInstance 	= 180000;
    public int 			maxUsageCountPerInstance			= 30;
    public int			randomUsageCountPerInstance			= 3;
    public int			sleepingDelay						= 4000;
    public int			shutdownDelay						= 1000;

    /** Stop daemon if an open office instance cannot be created */
    public boolean      exitsWhenInstanceCannotBeCreated   = true;

    public static final String CONFIG_TAG = "config : ";

    public boolean adminNeeded()
    {
        return adminAcceptor != null;
    }
    public int calculateMaxUsageCount()
    {
        return (int) Math.round(maxUsageCountPerInstance
                + (1. - 2 * Math.random()) * randomUsageCountPerInstance);
    }
    public int convertMillisToSeconds(int millis)
    {
        return (int) Math.round(millis * 0.001);
    }
    public void validate() throws Exception
    {
        File tmp;
        if (userInstallation.isEmpty()) throw new Exception(CONFIG_TAG + "no user installation found");
        if (acceptor == null || acceptor.equals("")) throw new Exception(CONFIG_TAG + "acceptor is empty" );
        tmp = new File(officeProgramDirectoryPath);
        if (!tmp.canRead()) throw new Exception(CONFIG_TAG + "office program directory path ("
                + officeProgramDirectoryPath + ") does not exist or is not readable");
    }
    /**
     * Returns a configuration in a shell manner
     * @see java.lang.Object#toString()
     * @return configuration informations
     */
    public String toString()
    {
        String str;

        str = "<CONFIGURATION>\n"
            + "\tadminAcceptor                       : " + adminAcceptor + "\n"
            + "\tacceptor                            : " + acceptor + "\n"
            + "\toffice program directory path       : " + officeProgramDirectoryPath + "\n";
        for (int i = 0; i < userInstallation.size(); i++)
            str += "\tuser installation                   : " + userInstallation.get(i) + "\n";
        str +=
              "\ttolerated startup time per instance : " + convertMillisToSeconds(toleratedStartupTimePerInstance) + "\n"
            + "\tmax usage count per instance        : " + maxUsageCountPerInstance + "\n"
            + "\trandom usage count per instance     : " + randomUsageCountPerInstance + "\n"
            + "\tsleeping delay                      : " + convertMillisToSeconds(sleepingDelay) + "\n"
            + "\tshutdown delay                      : " + convertMillisToSeconds(shutdownDelay) + "\n"
            + "</CONFIGURATION>\n";

        return str;
    }
}
