package org.nuxeo.ecm.core.event;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * Because {@link EventBundle} can be processed asynchronously, they can be executed:
 * <ul>
 * <li>in a different security context
 * <li>with a different {@link CoreSession}
 * </ul>
 *
 * This interface is used to mark Bundles that supports this kind of processing.
 * This basically means:
 * <ul>
 * <li>Create a JAAS session via Framework.login()
 * <li>Create a new usage {@link CoreSession}
 * <li>refetch any {@link EventContext} args / properties according to new session
 * <li>provide cleanup method
 * </ul>
 *
 * @author tiry
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
