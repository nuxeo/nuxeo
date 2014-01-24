package org.nuxeo.segment.io.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIOComponent;
import org.nuxeo.segment.io.SegmentIOMapper;

public class SegmenIOAsyncListener implements PostCommitEventListener {

    protected SegmentIOComponent getComponent() {
        return (SegmentIOComponent) Framework.getRuntime().getComponent(SegmentIOComponent.class.getName());
    }

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {

        SegmentIOComponent component = getComponent();

        List<String> eventToProcess = new ArrayList<String>();

        for (String event : component.getMappedEvents()) {
            if (bundle.containsEventName(event)) {
                eventToProcess.add(event);
            }
        }

        if (eventToProcess.size()>0) {
            Map<String, List<SegmentIOMapper>> event2Mappers = component.getMappers(eventToProcess);
            processEvents(event2Mappers, bundle);
        }

    }

    protected void processEvents(Map<String, List<SegmentIOMapper>> event2Mappers, EventBundle bundle) {

        for (Event event : bundle) {
            List<SegmentIOMapper> mappers = event2Mappers.get(event.getName());
            if (mappers==null || mappers.size()==0) {
                continue;
            }

        }
    }

}
