package org.nuxeo.ecm.platform.queue.api;

import java.net.URI;

public interface QueueNamer {

    /**
     * Build a queue URI
     *
     * @param name the queue name
     * @return the queue URI
     */
    URI newQueueName(String name);

    /**
     * Build a content name URI
     *
     * @param queueName the queue name
     * @param contentName the content name
     * @return the content URI
     */
    URI newContentName(String queueName, String contentName);

    /**
     * Build a content name URI
     *
     * @param name the base name
     * @param contentName the content name
     * @return the content URI
     */
    URI newContentName(URI name, String contentName);

}