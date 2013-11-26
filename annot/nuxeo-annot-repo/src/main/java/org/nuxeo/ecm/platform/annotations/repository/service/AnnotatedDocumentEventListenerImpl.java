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

package org.nuxeo.ecm.platform.annotations.repository.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.runtime.api.Framework;

public class AnnotatedDocumentEventListenerImpl implements
        AnnotatedDocumentEventListener {

    private static final Log log = LogFactory.getLog(AnnotatedDocumentEventListenerImpl.class);

    public void beforeAnnotationCreated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        // NOP
    }

    public void beforeAnnotationDeleted(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        // NOP
    }

    public void beforeAnnotationRead(NuxeoPrincipal principal,
            String annotationId) {
        // NOP
    }

    public void beforeAnnotationUpdated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        // NOP
    }

    public void afterAnnotationCreated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        notifyEvent(ANNOTATION_CREATED, annotation, documentLoc, principal);
    }

    public void afterAnnotationDeleted(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        notifyEvent(ANNOTATION_DELETED, annotation, documentLoc, principal);
    }

    public void afterAnnotationRead(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        // NOP for now
    }

    public void afterAnnotationUpdated(NuxeoPrincipal principal,
            DocumentLocation documentLoc, Annotation annotation) {
        notifyEvent(ANNOTATION_UPDATED, annotation, documentLoc, principal);
    }

    protected void notifyEvent(String eventId, Annotation annotation,
            DocumentLocation documentLocation, NuxeoPrincipal principal) {
        LoginContext loginContext = null;
        CoreSession session = null;
        String title = null;
        DocumentModel doc = null;
        try {
            loginContext = Framework.login();
            session = getSession(documentLocation.getServerName());
            if (session.exists(documentLocation.getDocRef())) {
                doc = session.getDocument(documentLocation.getDocRef());
                title = doc.getTitle();
            }

            Map<String, Serializable> properties = new HashMap<String, Serializable>();
            properties.put(AnnotatedDocumentEventListener.ANNOTATION_ID,
                    annotation.getId());
            properties.put(AnnotatedDocumentEventListener.ANNOTATION_SUBJECT,
                    annotation.getSubject());
            properties.put(AnnotatedDocumentEventListener.ANNOTATION_BODY,
                    annotation.getBodyAsText());

            EventContext ctx = null;
            if (doc != null) {
                DocumentEventContext docCtx = new DocumentEventContext(session,
                        principal, doc);
                docCtx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
                ctx = docCtx;
            } else {
                ctx = new EventContextImpl(session, principal);
            }
            ctx.setRepositoryName(documentLocation.getServerName());
            ctx.setProperties(properties);

            Event event = ctx.newEvent(eventId);
            Framework.getService(EventProducer.class).fireEvent(event);
        } catch (Exception e) {
            log.error("Unable to send the " + eventId + " event", e);
        } finally {
            if (session != null) {
                try {
                    session.save();
                } catch (ClientException e) {
                    log.error(String.format(
                            "error saving core session for annotation"
                                    + " event %d on document '%s'", eventId,
                            title), e);
                }
                CoreInstance.getInstance().close(session);
            }
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    log.error("Unable to logout: " + e.getMessage());
                }
            }
        }
    }

    protected CoreSession getSession(String repoName) throws Exception {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        Repository repository = repositoryManager.getRepository(repoName);
        return repository.open();
    }

}
