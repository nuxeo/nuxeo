package org.nuxeo.ecm.core.event.async;

import static java.util.Collections.singletonList;
import static org.nuxeo.ecm.core.event.async.EventsStreamListener.EVENT_STREAM;

import java.util.Map;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since xxx
 */
public class EventStreamTopology implements StreamProcessorTopology {

    public static final String ROUTER_NAME = "router";

    @Override
    public Topology getTopology(Map<String, String> options) {
        return Topology.builder()
                       .addComputation(() -> new EventRouterComputation(ROUTER_NAME, 1, 1),
                               singletonList("i1:" + EVENT_STREAM))
                       .build();
    }

}
