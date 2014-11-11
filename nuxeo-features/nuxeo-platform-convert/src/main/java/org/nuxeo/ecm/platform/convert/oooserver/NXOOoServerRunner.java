package org.nuxeo.ecm.platform.convert.oooserver;


import com.anwrt.ooserver.daemon.Config;
import com.anwrt.ooserver.daemon.Daemon;
import com.anwrt.ooserver.daemon.Log4JLogger;
import com.anwrt.ooserver.daemon.Logger;

public class NXOOoServerRunner implements Runnable {

    protected Config ooServerConfig=null;

    public NXOOoServerRunner(Config ooServerConfig) {
        this.ooServerConfig= ooServerConfig;
    }

    public void run() {
        Logger.newInstance(new Log4JLogger());
        Daemon daemon = new Daemon(ooServerConfig);
        daemon.run();
    }

}
