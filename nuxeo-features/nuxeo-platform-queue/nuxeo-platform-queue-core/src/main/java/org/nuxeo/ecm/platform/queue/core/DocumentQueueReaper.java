package org.nuxeo.ecm.platform.queue.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.platform.queue.api.QueueLocator;
import org.nuxeo.ecm.platform.queue.api.QueueManager;
import org.nuxeo.runtime.api.Framework;

public class DocumentQueueReaper implements EventListener {

    protected static final Log log = LogFactory.getLog(DocumentQueueReaper.class);

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!"queue-reaper-schedule".equals(event.getName())) {
            return;
        }
        QueueLocator locator = Framework.getLocalService(QueueLocator.class);
        for (QueueManager<?> mgr:locator.getManagers()) {
            mgr.purgeBlacklisted();
        }
    }

}
