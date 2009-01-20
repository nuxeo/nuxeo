package org.nuxeo.ecm.platform.convert.oooserver;


import com.anwrt.ooserver.daemon.Config;
import com.anwrt.ooserver.daemon.Daemon;

public class NXOOoServerRunner implements Runnable {

    protected Config ooServerConfig=null;

    public NXOOoServerRunner(Config ooServerConfig) {
        this.ooServerConfig= ooServerConfig;
    }

    public void run() {
        Daemon daemon = new Daemon(ooServerConfig);
        daemon.run();
    }

}
