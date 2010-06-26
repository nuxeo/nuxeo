package org.nuxeo.ecm.platform.queue.api;

import java.util.List;

/*
 *
 */

// TODO: Auto-generated Javadoc
/**
 * Provide access to atomic objects and services.
 * 
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public interface QueueManagerLocator {

    /**
     * Provide an access to the atomic queue that is in charge the provided
     * content.
     * 
     * @param content the content
     * @return queue
     */
    QueueManager locateQueue(QueueContent content)
            throws QueueNotFoundException;

    /**
     * Provide an access to the atomic queue.
     * 
     * @param type the type
     * @return queue
     */
    QueueManager locateQueue(String queueName) throws QueueNotFoundException;

    /**
     * Provide the list of registered queues
     * 
     * @return
     */
    List<String> getAvailableQueues();

}
