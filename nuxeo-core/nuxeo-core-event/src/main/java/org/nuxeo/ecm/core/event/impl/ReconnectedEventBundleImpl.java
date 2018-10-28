/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.event.impl;

import java.io.Serializable;
import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for an {@link EventBundle} that need to be reconnected to a usable Session.
 *
 * @author tiry
 */
public class ReconnectedEventBundleImpl implements ReconnectedEventBundle {

    private static final long serialVersionUID = 1L;

    protected EventBundle sourceEventBundle;

    /** Lister name or names. */
    protected String listenerName;

    protected transient List<Event> reconnectedEvents;

    protected transient LoginContext loginCtx;

    protected transient CloseableCoreSession reconnectedCoreSession;

    private static final Log log = LogFactory.getLog(ReconnectedEventBundleImpl.class);

    protected ReconnectedEventBundleImpl() {
    }

    public ReconnectedEventBundleImpl(EventBundle sourceEventBundle) {
        this.sourceEventBundle = sourceEventBundle;
    }

    /** @since 5.6 */
    public ReconnectedEventBundleImpl(EventBundle sourceEventBundle, String listenerName) {
        this.sourceEventBundle = sourceEventBundle;
        this.listenerName = listenerName;
    }

    protected CoreSession getReconnectedCoreSession(String repoName, String originatingUsername) {
        if (reconnectedCoreSession == null) {
            try {
                loginCtx = Framework.login();
            } catch (LoginException e) {
                log.error("Cannot log in", e);
                return null;
            }
            reconnectedCoreSession = CoreInstance.openCoreSessionSystem(repoName, originatingUsername);
        } else {
            // Sanity Check
            if (!reconnectedCoreSession.getRepositoryName().equals(repoName)) {
                if (repoName != null) {
                    throw new IllegalStateException("Can no reconnected a Bundle tied to several Core instances !");
                }
            }
        }
        return reconnectedCoreSession;
    }

    protected List<Event> getReconnectedEvents() {
        if (reconnectedEvents == null) {
            reconnectedEvents = new ArrayList<Event>();
            for (Event event : sourceEventBundle) {
                EventContext ctx = event.getContext();
                String repositoryName = ctx.getRepositoryName();
                CoreSession session;
                if (repositoryName == null) {
                    session = null;
                } else {
                    String originatingUsername = ctx.getPrincipal().getActingUser();
                    session = getReconnectedCoreSession(repositoryName, originatingUsername);
                }

                List<Object> newArgs = new ArrayList<Object>();
                for (Object arg : ctx.getArguments()) {
                    Object newArg = arg;
                    if (refetchDocumentModel(session, arg) && session.getPrincipal() != null) { // NOSONAR
                        DocumentModel oldDoc = (DocumentModel) arg;
                        DocumentRef ref = oldDoc.getRef();
                        if (ref != null) {
                            try {
                                if (session.exists(oldDoc.getRef())) {
                                    newArg = session.getDocument(oldDoc.getRef());
                                } else {
                                    // probably deleted doc
                                    newArg = new DeletedDocumentModel(oldDoc);
                                }
                            } catch (DocumentNotFoundException e) {
                                log.error("Can not refetch Doc with ref " + ref.toString(), e);
                            }
                        }
                    }
                    // XXX treat here other cases !!!!
                    newArgs.add(newArg);
                }

                EventContext newCtx = null;
                if (ctx instanceof DocumentEventContext) {
                    newCtx = new DocumentEventContext(session, ctx.getPrincipal(), (DocumentModel) newArgs.get(0),
                            (DocumentRef) newArgs.get(1));
                } else {
                    newCtx = new EventContextImpl(session, ctx.getPrincipal());
                    ((EventContextImpl) newCtx).setArgs(newArgs.toArray());
                }

                Map<String, Serializable> newProps = new HashMap<String, Serializable>();
                for (Entry<String, Serializable> prop : ctx.getProperties().entrySet()) {
                    Serializable propValue = prop.getValue();
                    if (refetchDocumentModel(session, propValue)) {
                        DocumentModel oldDoc = (DocumentModel) propValue;
                        DocumentRef oldRef = oldDoc.getRef();
                        try {
                            if (session.exists(oldRef)) { // NOSONAR
                                propValue = session.getDocument(oldRef);
                            } else {
                                log.warn("Listener " + (listenerName == null ? "" : "'" + listenerName + "' ")
                                        + "cannot refetch missing document: " + oldRef + " ("
                                        + oldDoc.getPathAsString() + ")");
                            }
                        } catch (DocumentNotFoundException e) {
                            log.error("Can not refetch Doc with ref " + oldRef, e);
                        }
                    }
                    // XXX treat here other cases !!!!
                    newProps.put(prop.getKey(), propValue);
                }
                newCtx.setProperties(newProps);
                Event newEvt = new EventImpl(event.getName(), newCtx, event.getFlags(), event.getTime());
                reconnectedEvents.add(newEvt);
            }
        }
        return reconnectedEvents;
    }

    protected boolean refetchDocumentModel(CoreSession session, Object eventProperty) {
        if (eventProperty instanceof DocumentModel && session != null) {
            DocumentModel doc = (DocumentModel) eventProperty;
            if (Boolean.TRUE.equals(doc.getContextData(SKIP_REFETCH_DOCUMENT_CONTEXT_KEY))) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String getName() {
        return sourceEventBundle.getName();
    }

    @Override
    public VMID getSourceVMID() {
        return sourceEventBundle.getSourceVMID();
    }

    @Override
    public boolean hasRemoteSource() {
        return sourceEventBundle.hasRemoteSource();
    }

    @Override
    public boolean isEmpty() {
        return sourceEventBundle.isEmpty();
    }

    @Override
    public Event peek() {
        return getReconnectedEvents().get(0);
    }

    @Override
    public void push(Event event) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return sourceEventBundle.size();
    }

    @Override
    public Iterator<Event> iterator() {
        return getReconnectedEvents().iterator();
    }

    @Override
    public void disconnect() {
        if (reconnectedCoreSession != null) {
            reconnectedCoreSession.close();
        }
        reconnectedCoreSession = null;
        reconnectedEvents = null;
        if (loginCtx != null) {
            try {
                loginCtx.logout();
            } catch (LoginException e) {
                log.error("Cannot log out", e);
            } finally {
                loginCtx = null;
            }
        }
    }

    @Override
    public boolean containsEventName(String eventName) {
        return sourceEventBundle.containsEventName(eventName);
    }

    public List<String> getEventNames() {
        List<String> eventNames = new ArrayList<String>();
        for (Event event : sourceEventBundle) {
            eventNames.add(event.getName());
        }
        return eventNames;
    }
}
