package org.nuxeo.ecm.platform.importer.queue.tests;

import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.producer.AbstractProducer;

import java.util.Random;

/**
 * This should produce documents and buggy nodes. Some nodes that rollback the transaction and some nodes that throws an
 * exception.
 *
 * @since 8.3
 */
public class BuggyNodeProducer extends AbstractProducer {

    private final int producerDelayMs;
    private int nbNode;

    private int exceptionFrequency;

    private int rollBackFrequency;

    public BuggyNodeProducer(ImporterLogger logger, int nbNode, int rollBackFrequency, int exceptionFrequency, int producerDelayMs) {
        super(logger);
        this.nbNode = nbNode;
        this.rollBackFrequency = rollBackFrequency;
        this.exceptionFrequency = exceptionFrequency;
        this.producerDelayMs = producerDelayMs;
    }

    @Override
    public void run() {
        started = true;
        try {
            for (int i = 0; i < nbNode; i++) {
                if (producerDelayMs > 0) {
                    Thread.sleep((new Random()).nextInt(producerDelayMs));
                }
                dispatch(new BuggySourceNode(i, i % rollBackFrequency == 0, i % exceptionFrequency == 0));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            completed = true;
        }
    }

}
