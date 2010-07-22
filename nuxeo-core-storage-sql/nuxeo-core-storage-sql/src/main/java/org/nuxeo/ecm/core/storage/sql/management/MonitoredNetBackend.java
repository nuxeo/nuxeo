package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.storage.sql.net.NetBackend;

public class MonitoredNetBackend extends MonitoredBackend {

    public MonitoredNetBackend() {
       super(new NetBackend());
    }

}
