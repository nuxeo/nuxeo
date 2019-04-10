package org.nuxeo.segment.io.listener;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.segment.io.SegmentIO;
import org.nuxeo.segment.io.SegmentIOMapper;

public class SegmentIOAsyncListener implements PostCommitEventListener {

    private static Log log = LogFactory.getLog(SegmentIOAsyncListener.class);

    @Override
    public void handleEvent(EventBundle bundle) throws ClientException {

        SegmentIO service = Framework.getService(SegmentIO.class);

        List<String> eventToProcess = new ArrayList<String>();

        for (String event : service.getMappedEvents()) {
            if (bundle.containsEventName(event)) {
                eventToProcess.add(event);
            }
        }

        if (eventToProcess.size() > 0) {
            Map<String, List<SegmentIOMapper>> event2Mappers = service.getMappers(eventToProcess);
            processEvents(event2Mappers, bundle);
        }

    }

    protected void processEvents(Map<String, List<SegmentIOMapper>> event2Mappers, EventBundle bundle) {

        for (Event event : bundle) {
            List<SegmentIOMapper> mappers = event2Mappers.get(event.getName());
            if (mappers == null || mappers.size() == 0) {
                continue;
            }

            for (SegmentIOMapper mapper : mappers) {

                Map<String, Object> ctx = new HashMap<String, Object>();

                Principal princ = event.getContext().getPrincipal();
                NuxeoPrincipal principal = null;
                if (princ instanceof NuxeoPrincipal) {
                    principal = (NuxeoPrincipal) princ;
                } else {
                    try {
                        principal = Framework.getLocalService(UserManager.class).getPrincipal(princ.getName());
                    } catch (ClientException e) {
                        log.error("Unable to resolve principal for name " + princ.getName());
                        continue;
                    }
                }

                ctx.put("event", event);
                ctx.put("eventContext", event.getContext());
                ctx.put("principal", principal);
                if (event.getContext() instanceof DocumentEventContext) {
                    DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
                    ctx.put("doc", docCtx.getSourceDocument());
                    ctx.put("repository", docCtx.getRepositoryName());
                    ctx.put("session", docCtx.getCoreSession());
                    ctx.put("dest", docCtx.getDestination());
                }
                Map<String, Serializable> mapped = mapper.getMappedData(ctx);

                if (mapper.isIdentify()) {
                    Framework.getService(SegmentIO.class).identify(principal, mapped);
                } else {
                    Framework.getService(SegmentIO.class).track(principal, event.getName(), mapped);
                }

            }
        }
    }

}
