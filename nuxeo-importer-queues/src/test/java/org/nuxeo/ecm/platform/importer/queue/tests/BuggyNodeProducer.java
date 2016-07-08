package org.nuxeo.ecm.platform.importer.queue.tests;

import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.ecm.platform.importer.queue.producer.AbstractProducer;

import java.util.Random;

/**
 * This should produce documents and buggy nodes. Some nodes that rollback the transaction and some nodes that throws an
 * exception.
 *
 * @since 8.3
 */
public class BuggyNodeProducer extends AbstractProducer {

    private int nbNode;

    private QueuesManager qm;

    private int exceptionFrequency;

    private int rollBackFrequency;

    public BuggyNodeProducer(ImporterLogger logger, int nbNode, int rollBackFrequency, int exceptionFrequency) {
        super(logger);
        this.nbNode = nbNode;
        this.rollBackFrequency = rollBackFrequency;
        this.exceptionFrequency = exceptionFrequency;
    }

    @Override
    public void run() {
        started = true;
        try {
            for (int i = 0; i < nbNode; i++) {
                Thread.sleep(new Random().nextInt(100));
                dispatch(new BuggySourceNode(i, i % rollBackFrequency == 0, i % exceptionFrequency == 0));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            completed = true;
        }
    }

}
