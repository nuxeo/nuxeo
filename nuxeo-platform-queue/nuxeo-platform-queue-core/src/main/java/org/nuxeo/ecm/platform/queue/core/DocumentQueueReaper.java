package org.nuxeo.ecm.platform.queue.core;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.management.storage.DocumentStoreSessionRunner;

public class DocumentQueueReaper implements EventListener {

    protected static final Log log = LogFactory.getLog(DocumentQueueReaper.class);

    protected static String formatTimestamp(Calendar cal) {
        return new SimpleDateFormat("'TIMESTAMP' ''yyyy-MM-dd HH:mm:ss.SSS''").format(cal.getTime());
    }

    @Override
    public void handleEvent(Event event) throws ClientException {
        if (!"queue-reaper-schedule".equals(event.getName())) {
            return;
        }
        new DocumentStoreSessionRunner() {
            @Override
            public void run() throws ClientException {
                Calendar calendar = Calendar.getInstance();
                calendar.roll(Calendar.MINUTE, 10);
                String ts = formatTimestamp(calendar);
                log.debug("Removing blacklisted doc oldest than " + ts);
                String req = String.format("SELECT * from QueueItem where qitm:blacklistTime < %s and ecm:isProxy = 0", ts);
                DocumentModelList docs = session.query(req);
                for (DocumentModel doc:docs) {
                    log.debug("Removing blacklisted doc " + doc.getPathAsString());
                    session.removeDocument(doc.getRef());
                }
            }
        }.runSafe();
    }

}
