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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class AnnotatedDocumentEventListenerImpl implements
        AnnotatedDocumentEventListener {

    private static final Log log = LogFactory.getLog(AnnotatedDocumentEventListenerImpl.class);

    private static final String ANNOTATION_CREATED = "annotationCreated";

    private static final String ANNOTATION_UPDATED = "annotationUpdated";

    private static final String ANNOTATION_DELETED = "annotationDeleted";

    private transient EventService eventService;

    public void beforeAnnotationCreated(DocumentLocation documentLoc,
            Annotation annotation) {
        // NOP
    }

    public void beforeAnnotationDeleted(DocumentLocation documentLoc,
            Annotation annotation) {
        // NOP
    }

    public void beforeAnnotationRead(String annId) {
        // NOP
    }

    public void beforeAnnotationUpdated(DocumentLocation documentLoc,
            Annotation annotation) {
        // NOP
    }

    public void afterAnnotationCreated(DocumentLocation documentLoc,
            Annotation annotation) {
        notifyEvent(ANNOTATION_CREATED, annotation, documentLoc);
    }

    public void afterAnnotationDeleted(DocumentLocation documentLoc,
            Annotation annotation) {
        notifyEvent(ANNOTATION_DELETED, annotation, documentLoc);
    }

    public void afterAnnotationRead(DocumentLocation documentLoc,
            Annotation annotation) {
        // NOP for now
    }

    public void afterAnnotationUpdated(DocumentLocation documentLoc,
            Annotation annotation) {
        notifyEvent(ANNOTATION_UPDATED, annotation, documentLoc);
    }

    protected void notifyEvent(String eventId, Annotation annotation,
            DocumentLocation documentLocation) {
        try {
            DocumentModel dm = getDocument(documentLocation);
            Map<String, Serializable> properties = new HashMap<String, Serializable>();

            DocumentEventContext ctx = new DocumentEventContext(null,
                    new NuxeoPrincipalImpl(annotation.getCreator()), dm);
            ctx.setRepositoryName(dm.getRepositoryName());
            ctx.setProperties(properties);
            ctx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);

            Event event = ctx.newEvent(eventId);
            EventProducer evtProducer;
            evtProducer = Framework.getService(EventProducer.class);
            evtProducer.fireEvent(event);
        } catch (Exception e) {
            log.error("Unable to send the " + eventId + " event", e);
        }
    }

    protected DocumentModel getDocument(DocumentLocation docLoc)
            throws Exception {
        LoginContext loginContext = null;
        DocumentModel doc = null;

        try {
            loginContext = Framework.login();
            CoreSession session = getSession(docLoc.getServerName());
            doc = session.getDocument(docLoc.getDocRef());
            CoreInstance.getInstance().close(session);
        } finally {
            if (loginContext != null) {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    log.error("Unable to logout: " + e.getMessage());
                }
            }
        }
        return doc;
    }

    protected CoreSession getSession(String repoName) throws Exception {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        Repository repository = repositoryManager.getRepository(repoName);
        return repository.open();
    }

}
