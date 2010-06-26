package org.nuxeo.ecm.platform.queue.api;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Content that needs long processing. Asynchronous operation is started by the
 * queue when the content is handled. Content will remain in queue until it's
 * removed by an end of asynchronous operation call.
 *
 * @see QueueManager
 **/

final public class QueueContent {

    final public static long DEFAULT_DELAY = 1000;

    public QueueContent(URI owner, String destination, String name) {
        this.owner = owner;
        this.destination = destination;
        this.name = name;
        delay = DEFAULT_DELAY;
    }

    final URI owner;

    final String destination;

    final String name;

    long delay;

    String comments;

    /**
     * return the
     *
     * @return
     */
    public URI getResourceURI() throws URISyntaxException {
        return new URI("queueContent:" + destination + ":" + name);
    }

    /**
     * Gives the user who is performing the job
     *
     * @return
     */
    public URI getOwner() {
        return owner;
    }

    /**
     * Gives the queue name on which the content should be handled
     *
     * @return
     */

    public String getDestination() {
        return destination;
    }

    /**
     * Uniquely names the content inside a queue.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gives the delay for locking purpose
     *
     * @return
     */
    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    /**
     * Gives information about the task being processed
     *
     * @return
     */
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }


}
