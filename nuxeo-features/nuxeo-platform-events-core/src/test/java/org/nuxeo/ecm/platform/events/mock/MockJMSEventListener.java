package org.nuxeo.ecm.platform.events.mock;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.listener.JMSEventListener;

public class MockJMSEventListener extends JMSEventListener {

    private DocumentMessageProducer service;

    public void reset()
    {
        eventsStack.clear();
        service=null;
    }

    public void fakeSendCoreMessage(String eventId, String sessionId, String docName)
    {
        Map<String, Object> options = new HashMap<String, Object>();
        DocumentModelImpl doc = new DocumentModelImpl("File");
        if (docName!=null)
            doc.setPathInfo("/root/", docName);
        options.put(CoreEventConstants.SESSION_ID, sessionId);
        CoreEvent coreEvent = new CoreEventImpl(eventId, doc, options,
                null, DocumentEventCategories.EVENT_DOCUMENT_CATEGORY, "fake");

        super.notifyEvent(coreEvent);
    }

    public DocumentMessageProducer getProducerService() {
        if (service==null)
            service= new MockMessageProducer();
        return service;
    }

    public int getStackedMessageCount(String sessionId)
    {
        return super.getStackForSessionId(sessionId).size();
    }

}
