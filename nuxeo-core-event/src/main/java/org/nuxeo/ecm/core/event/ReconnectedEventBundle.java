package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Because {@link EventBundle} can be processed asynchronously, they can be executed :
 *  - in a different security context
 *  - with a different {@link CoreSession}
 *  This interface is used to mark Bundles that supports this kind of processing.
 *  This basically means :
 *   - Create a JAAS session via Framework.login()
 *   - Create a new usage {@link CoreSession}
 *   - refetch any {@link EventContext} args / properties according to new session
 *   - provide cleanup method
 *
 * @author tiry
 *
 */
public interface ReconnectedEventBundle extends EventBundle {

    /**
     * Manage cleanup after processing
     */
    void disconnect();

    /**
     * marker for Bundles comming from JMS
     * @return
     */
    boolean comesFromJMS();
}
