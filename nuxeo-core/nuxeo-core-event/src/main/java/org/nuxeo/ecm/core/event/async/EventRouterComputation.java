package org.nuxeo.ecm.core.event.async;

import java.util.List;

import org.nuxeo.lib.stream.computation.AbstractBatchComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;

/**
 * Router for event stream.
 *
 * @since xxx
 */
public class EventRouterComputation extends AbstractBatchComputation {

    public EventRouterComputation(String name, int nbInputStreams, int nbOutputStreams) {
        super(name, nbInputStreams, nbOutputStreams);
    }

    @Override
    protected void batchProcess(ComputationContext context, String inputStreamName, List<Record> records) {

    }

    @Override
    public void batchFailure(ComputationContext context, String inputStreamName, List<Record> records) {

    }

}
