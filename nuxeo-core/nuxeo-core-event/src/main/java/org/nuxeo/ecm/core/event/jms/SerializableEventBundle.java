/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */

package org.nuxeo.ecm.core.event.jms;

import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventBundleImpl;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;

/**
 * Serializable representation of an {@link EventBundle} that is used for JMS forwarding.
 *
 * @author Thierry Delprat
 */
public class SerializableEventBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SerializableEventBundle.class);

    protected final List<Map<String, Serializable>> serialisableEvents;

    protected final String eventBundleName;

    protected final VMID sourceVMID;

    protected boolean isDocumentEventContext = false;

    protected String coreInstanceName;

    public SerializableEventBundle(EventBundle events) {
        eventBundleName = events.getName();
        sourceVMID = events.getSourceVMID();
        serialisableEvents = new ArrayList<Map<String, Serializable>>();

        for (Event event : events) {
            if (event.isLocal()) {
                // local event should not be exported to JMS
                continue;
            }
            CoreSession evtSession = event.getContext().getCoreSession();

            String repoName = null;
            if (evtSession != null) {
                repoName = evtSession.getRepositoryName();
                if (coreInstanceName == null) {
                    coreInstanceName = repoName;
                }
            }

            Map<String, Serializable> serializableEvent = new HashMap<String, Serializable>();

            serializableEvent.put("name", event.getName());
            serializableEvent.put("time", Long.toString(event.getTime()));
            serializableEvent.put("contextProperties", (Serializable) event.getContext().getProperties());
            if (evtSession != null) {
                serializableEvent.put("contextSessionId", evtSession.getSessionId());
            }
            serializableEvent.put("principal", event.getContext().getPrincipal().getName());

            serializableEvent.put("contextSessionRepositoryName", repoName);

            if (event.getContext() instanceof DocumentEventContext) {
                serializableEvent.put("isDocumentEventContext", true);
            } else {
                serializableEvent.put("isDocumentEventContext", false);
            }

            Object[] args = event.getContext().getArguments();
            List<Serializable> listArgs = new ArrayList<Serializable>();
            for (Object arg : args) {
                if (arg instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) arg;
                    String strRepresentation = doc.getRepositoryName() + ":" + doc.getId() + ":" + doc.getType() + ":"
                            + doc.getPathAsString();
                    listArgs.add("DOCREF:" + strRepresentation);
                } else if (arg instanceof Serializable) {
                    log.debug("Adding serializable argument of class " + arg.getClass().getCanonicalName());
                    listArgs.add((Serializable) arg);
                } else {
                    listArgs.add(null);
                }
            }

            serializableEvent.put("args", (Serializable) listArgs);
            serialisableEvents.add(serializableEvent);
        }
    }

    // Should not be necessary since this is noww done in CoreSession
    protected Map<String, Serializable> filterContextProperties(Map<String, Serializable> properties) {
        Map<String, Serializable> serializableProps = new HashMap<String, Serializable>();

        for (String key : properties.keySet()) {
            Object value = properties.get(key);
            if (value instanceof Serializable) {
                Serializable serializableValue = (Serializable) value;
                serializableProps.put(key, serializableValue);
            } else {
                log.error("ContextMap contains non serializable object under key " + key);
            }
        }
        return serializableProps;
    }

    public VMID getSourceVMID() {
        return sourceVMID;
    }

    public String getEventBundleName() {
        return eventBundleName;
    }

    public String getCoreInstanceName() {
        return coreInstanceName;
    }

    public class EventBundleRelayedViaJMS extends EventBundleImpl {
        private static final long serialVersionUID = 1L;

        public EventBundleRelayedViaJMS() {
            // init VMID
            super(sourceVMID);
        }
    }

    @SuppressWarnings("unchecked")
    public EventBundle reconstructEventBundle(CoreSession session) throws CannotReconstruct {

        if (!session.getRepositoryName().equals(coreInstanceName)) {
            throw new CannotReconstruct("This session can not be used on this Bundle");
        }
        EventBundle bundle = new EventBundleRelayedViaJMS();

        if (serialisableEvents == null) {
            return null;
        }

        for (Map<String, Serializable> evt : serialisableEvents) {

            String eventName = (String) evt.get("name");
            Long time = Long.parseLong((String) evt.get("time"));

            Map<String, Serializable> ctxProperties = (Map<String, Serializable>) evt.get("contextProperties");
            NuxeoPrincipal principal = new UserPrincipal((String) evt.get("principal"), null, false, false);

            List<Serializable> listArgs = (List<Serializable>) evt.get("args");

            Object[] args = new Object[listArgs.size()];

            int idx = 0;
            for (Serializable sArg : listArgs) {
                Object value;
                if (sArg == null) {
                    value = null;
                } else if (sArg instanceof String) {
                    String arg = (String) sArg;
                    if (arg.startsWith("DOCREF:")) {
                        String[] part = arg.split(":");
                        DocumentRef idRef = new IdRef(part[2]);
                        DocumentModel doc = null;
                        if (session.exists(idRef)) {
                            doc = session.getDocument(idRef);
                        } else {
                            String parentPath = new Path(part[4]).removeLastSegments(1).toString();
                            doc = new DocumentModelImpl(session.getSessionId(), part[3], part[2], new Path(part[4]),
                                    null, idRef, new PathRef(parentPath), null, null, null, null);
                        }
                        value = doc;
                    } else {
                        value = arg;
                    }
                } else {
                    value = sArg;
                }
                args[idx] = value;
                idx++;
            }

            EventContext ctx;
            if ((Boolean) evt.get("isDocumentEventContext")) {
                ctx = new DocumentEventContext(session, principal, (DocumentModel) args[0], (DocumentRef) args[1]);
                // XXX we loose other args ...
            } else {
                ctx = new EventContextImpl(session, principal);
                ((EventContextImpl) ctx).setArgs(args);
            }

            ctx.setProperties(ctxProperties);
            Event e = new EventImpl(eventName, ctx, Event.FLAG_NONE, time);
            bundle.push(e);
        }
        return bundle;
    }

    public static class CannotReconstruct extends NuxeoException {

        private static final long serialVersionUID = 1L;

        public CannotReconstruct(String message) {
            super(message);
        }

    }

}
