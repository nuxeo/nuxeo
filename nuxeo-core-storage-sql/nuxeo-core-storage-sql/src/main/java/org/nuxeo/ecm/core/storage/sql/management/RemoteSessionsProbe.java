package org.nuxeo.ecm.core.storage.sql.management;

import org.nuxeo.ecm.core.management.statuses.Probe;
import org.nuxeo.ecm.core.management.statuses.ProbeStatus;

public class RemoteSessionsProbe implements Probe {

    @Override
    public void init(Object service) {

    }

    @Override
    public ProbeStatus run()  {
       RepositoryStatus status = new RepositoryStatus();
       return ProbeStatus.newSuccess(status.listRemoteSessions());
    }

}
