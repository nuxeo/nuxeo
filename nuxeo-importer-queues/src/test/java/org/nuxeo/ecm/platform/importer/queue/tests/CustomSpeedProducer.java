package org.nuxeo.ecm.platform.importer.queue.tests;

import java.util.concurrent.TimeUnit;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.producer.AbstractProducer;

public class CustomSpeedProducer extends AbstractProducer {

    private int nbNode;

    private int speed;

    public CustomSpeedProducer(ImporterLogger log, int nbNode, int speed) {
        super(log);
        this.nbNode = nbNode;
        this.speed = speed;
    }

    @Override
    public void run() {
        started = true;
        log.error("Start dispatcher");
        for (int i = 0; i < nbNode; i++) {
            try {
                BuggySourceNode node = new BuggySourceNode(i, false, false);
                dispatch(node);
                log.error("Dispatching node " + node.getName());
                if (speed > 0) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(speed));
                }
            } catch (InterruptedException e) {
                ExceptionUtils.checkInterrupt(e);
            }
        }
        completed = true;
        log.error("Dispatcher completed");
        canStop = true;
    }

}
