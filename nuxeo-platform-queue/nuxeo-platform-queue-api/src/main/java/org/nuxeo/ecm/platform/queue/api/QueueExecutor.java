package org.nuxeo.ecm.platform.queue.api;

/**
 * Let system invokes the long asynchronous operation. It's the user authority
 * to call-back handler for notifying operation termination.
 * 
 * @param queue the queue
 */
public interface QueueExecutor {

    void execute(QueueContent content, QueueHandler handler);

}
