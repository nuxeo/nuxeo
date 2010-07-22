package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;

public class MonitoredJDBCBackend extends MonitoredBackend {

    public MonitoredJDBCBackend() {
       super(new JDBCBackend());
    }

}
