package org.nuxeo.runtime.jtajca;

import org.apache.geronimo.connector.outbound.AbstractConnectionManager;

public interface NuxeoContainerListener {

    void handleNewConnectionManager(String repositoryName, AbstractConnectionManager mgr);

    void handleConnectionManagerReset(String repositoryName, AbstractConnectionManager mgr);
}
