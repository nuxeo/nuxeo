/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.jms;

import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SimplePrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
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
 * @author tiry
 */
public class JMSEventBundle implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(JMSEventBundle.class);

    protected final List<Map<String, Serializable>> serialisableEvents;

    protected final String eventBundleName;

    protected final VMID sourceVMID;

    protected boolean isDocumentEventContext = false;

    protected String coreInstanceName;

    public JMSEventBundle(EventBundle events) {
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
                DocumentEventContext docContext = (DocumentEventContext) event.getContext();
                serializableEvent.put("isDocumentEventContext", true);
            } else {
                serializableEvent.put("isDocumentEventContext", false);
            }

            Object[] args = event.getContext().getArguments();
            List<Serializable> listArgs = new ArrayList<Serializable>();
            for (Object arg : args) {
                if (arg instanceof DocumentModel) {
                    DocumentModel doc = (DocumentModel) arg;
                    String strRepresentation = doc.getRepositoryName() + ":" + doc.getId() + ":" + doc.getType() + ":" + doc.getPathAsString();
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
        public EventBundleRelayedViaJMS() {
            // init VMID
            super(sourceVMID);
        }
    }

    public EventBundle reconstructEventBundle(CoreSession session) throws CannotReconstructEventBundle {

        if (!session.getRepositoryName().equals(coreInstanceName)) {
            throw new CannotReconstructEventBundle("This session can not be used on this Bundle");
        }
        EventBundle bundle = new EventBundleRelayedViaJMS();

        if (serialisableEvents == null) {
            return null;
        }

        for (Map<String, Serializable> evt : serialisableEvents) {

            String eventName = (String) evt.get("name");
            Long time = Long.parseLong((String) evt.get("time"));

            Map<String, Serializable> ctxProperties = (Map<String, Serializable>) evt.get("contextProperties");
            Principal principal = new SimplePrincipal((String) evt.get("principal"));

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
                        try {
                            if (session != null && session.exists(idRef)) {
                                doc = session.getDocument(idRef);
                            } else {
                                String parentPath = new Path(part[4]).removeLastSegments(1).toString();
                                doc = new DocumentModelImpl(
                                        session.getSessionId(), part[3], part[2], new Path(part[4]), idRef, new PathRef(parentPath), null, null);
                            }
                        }
                        catch (ClientException e) {
                            // TODO
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
                ctx = new DocumentEventContext(
                        session, principal, (DocumentModel) args[0], (DocumentRef) args[1]);
                // XXX we loose other args ...
            } else {
                ctx = new EventContextImpl(session, principal);
                ((EventContextImpl) ctx).setArgs(args);
            }

            ctx.setProperties(ctxProperties);
            Event e = new EventImpl(eventName, ctx, null, time);
            bundle.push(e);
        }
        return bundle;
    }

}
