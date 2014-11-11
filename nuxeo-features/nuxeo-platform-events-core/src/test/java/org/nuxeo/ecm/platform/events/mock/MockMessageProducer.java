package org.nuxeo.ecm.platform.events.mock;

import java.util.List;

import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.EventMessage;
import org.nuxeo.ecm.platform.events.api.NXCoreEvent;

public class MockMessageProducer implements DocumentMessageProducer {

    public int producedMessages=0;
    public int duplicatedMessages=0;

    public void produce(DocumentMessage message) {
        // TODO Auto-generated method stub

    }

    public void produce(EventMessage message) {
        // TODO Auto-generated method stub

    }

    public void produce(NXCoreEvent event) {
        // TODO Auto-generated method stub

    }

    public void produceCoreEvents(List<NXCoreEvent> events) {
        // TODO Auto-generated method stub

    }

    public void produceEventMessages(List<EventMessage> messages) {

        for(EventMessage evt : messages)
        {
            producedMessages+=1;

            Boolean dup = (Boolean) evt.getEventInfo().get(EventMessage.DUPLICATED);
            if (dup!=null && dup==true)
            {
                duplicatedMessages+=1;
            }
        }

    }

}
