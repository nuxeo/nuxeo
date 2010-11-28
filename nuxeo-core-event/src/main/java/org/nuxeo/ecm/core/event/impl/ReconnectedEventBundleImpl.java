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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.DeletedDocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for an {@link EventBundle} that need to be reconnected
 * to a usable Session.
 *
 * @author tiry
 */
public class ReconnectedEventBundleImpl implements ReconnectedEventBundle {

    private static final long serialVersionUID = 1L;

    protected EventBundle sourceEventBundle;

    protected List<Event> reconnectedEvents;

    protected LoginContext loginCtx;

    protected CoreSession reconnectedCoreSession;

    private static final Log log = LogFactory.getLog(ReconnectedEventBundleImpl.class);

    public ReconnectedEventBundleImpl() {
    }

    public ReconnectedEventBundleImpl(EventBundle sourceEventBundle) {
        this.sourceEventBundle = sourceEventBundle;
    }

    protected CoreSession getReconnectedCoreSession(String repoName) {
        if (reconnectedCoreSession == null) {
            try {
                loginCtx = Framework.login();
            } catch (LoginException e) {
                log.error("Can not connect", e);
                return null;
            }

            try {
                RepositoryManager mgr = Framework.getService(RepositoryManager.class);
                Repository repo;
                if (repoName != null) {
                    repo = mgr.getRepository(repoName);
                } else {
                    repo = mgr.getDefaultRepository();
                    repoName = repo.getName();
                }

                reconnectedCoreSession = repo.open();
            } catch (Exception e) {
                log.error("Error while openning core session on repo "
                        + repoName, e);
                return null;
            }
        } else {
            // Sanity Check
            if (!reconnectedCoreSession.getRepositoryName().equals(repoName)) {
                if (repoName != null) {
                    throw new IllegalStateException(
                            "Can no reconnected a Bundle tied to several Core instances !");
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
                CoreSession session = ctx.getRepositoryName() == null ? null
                        : getReconnectedCoreSession(ctx.getRepositoryName());

                List<Object> newArgs = new ArrayList<Object>();
                for (Object arg : ctx.getArguments()) {
                    Object newArg = arg;
                    if (arg instanceof DocumentModel && session != null
                            && session.getPrincipal() != null) {
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
                            } catch (ClientException e) {
                                log.error("Can not refetch Doc with ref "
                                        + ref.toString(), e);
                            }
                        }
                    }
                    // XXX treat here other cases !!!!
                    newArgs.add(newArg);
                }

                EventContext newCtx = null;
                if (ctx instanceof DocumentEventContext) {
                    newCtx = new DocumentEventContext(session,
                            ctx.getPrincipal(), (DocumentModel) newArgs.get(0),
                            (DocumentRef) newArgs.get(1));
                } else {
                    newCtx = new EventContextImpl(session, ctx.getPrincipal());
                    ((EventContextImpl) newCtx).setArgs(newArgs.toArray());
                }

                Map<String, Serializable> newProps = new HashMap<String, Serializable>();
                for (Entry<String, Serializable> prop : ctx.getProperties().entrySet()) {
                    Serializable propValue = prop.getValue();
                    if (propValue instanceof DocumentModel && session != null) {
                        DocumentModel oldDoc = (DocumentModel) propValue;
                        try {
                            propValue = session.getDocument(oldDoc.getRef());
                        } catch (ClientException e) {
                            log.error("Can not refetch Doc with ref "
                                    + oldDoc.getRef().toString(), e);
                        }
                    }
                    // XXX treat here other cases !!!!
                    newProps.put(prop.getKey(), propValue);
                }
                newCtx.setProperties(newProps);
                Event newEvt = new EventImpl(event.getName(), newCtx,
                        event.getFlags(), event.getTime());
                reconnectedEvents.add(newEvt);
            }
        }
        return reconnectedEvents;
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
            CoreInstance.getInstance().close(reconnectedCoreSession);
        }
        if (loginCtx != null) {
            try {
                loginCtx.logout();
            } catch (LoginException e) {
                log.error("Error while logging out", e);
            }
        }
    }

    @Override
    public boolean comesFromJMS() {
        return false;
    }

    @Override
    public boolean containsEventName(String eventName) {
        return sourceEventBundle.containsEventName(eventName);
    }

}
