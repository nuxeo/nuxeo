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
public class BuggyNodeProducer extends AbstractProducer<BuggySourceNode> {

    private final int producerDelayMs;
    private final int nbNode;
    private final int exceptionFrequency;
    private final int rollBackFrequency;
    private final int exceptionInProducer;

    public BuggyNodeProducer(ImporterLogger logger, int nbNode, int rollBackFrequency, int exceptionFrequency, int producerDelayMs, int exceptionInProducer) {
        super(logger);
        this.nbNode = nbNode;
        this.rollBackFrequency = rollBackFrequency;
        this.exceptionFrequency = exceptionFrequency;
        this.producerDelayMs = producerDelayMs;
        this.exceptionInProducer = exceptionInProducer;
    }

    @Override
    public void run() {
        started = true;
        try {
            for (int i = 0; i < nbNode; i++) {
                if (producerDelayMs > 0) {
                    Thread.sleep((new Random()).nextInt(producerDelayMs));
                }
                if (exceptionInProducer > 0 && i >= exceptionInProducer) {
                    throw new RuntimeException("This is a buggy exception during producer processing !");
                }
                dispatch(new BuggySourceNode(i, rollBackFrequency > 0 ? i % rollBackFrequency == 0: false,
                        exceptionFrequency > 0 ? i % exceptionFrequency == 0 : false));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            completed = true;
        }
    }

}
