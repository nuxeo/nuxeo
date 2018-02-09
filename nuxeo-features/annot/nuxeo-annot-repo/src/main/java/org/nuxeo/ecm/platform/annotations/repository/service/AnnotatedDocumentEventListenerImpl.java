/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.annotations.repository.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.runtime.api.Framework;

public class AnnotatedDocumentEventListenerImpl implements AnnotatedDocumentEventListener {

    private static final Log log = LogFactory.getLog(AnnotatedDocumentEventListenerImpl.class);

    public void beforeAnnotationCreated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        // NOP
    }

    public void beforeAnnotationDeleted(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        // NOP
    }

    public void beforeAnnotationRead(NuxeoPrincipal principal, String annotationId) {
        // NOP
    }

    public void beforeAnnotationUpdated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        // NOP
    }

    public void afterAnnotationCreated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        notifyEvent(ANNOTATION_CREATED, annotation, documentLoc, principal);
    }

    public void afterAnnotationDeleted(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        notifyEvent(ANNOTATION_DELETED, annotation, documentLoc, principal);
    }

    public void afterAnnotationRead(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        // NOP for now
    }

    public void afterAnnotationUpdated(NuxeoPrincipal principal, DocumentLocation documentLoc, Annotation annotation) {
        notifyEvent(ANNOTATION_UPDATED, annotation, documentLoc, principal);
    }

    protected void notifyEvent(String eventId, Annotation annotation, DocumentLocation documentLocation,
            NuxeoPrincipal principal) {
        if (documentLocation == null) {
            return;
        }
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(documentLocation.getServerName())) {
            DocumentModel doc = null;
            if (session.exists(documentLocation.getDocRef())) {
                doc = session.getDocument(documentLocation.getDocRef());
            }

            Map<String, Serializable> properties = new HashMap<String, Serializable>();
            properties.put(AnnotatedDocumentEventListener.ANNOTATION_ID, annotation.getId());
            properties.put(AnnotatedDocumentEventListener.ANNOTATION_SUBJECT, annotation.getSubject());
            properties.put(AnnotatedDocumentEventListener.ANNOTATION_BODY, annotation.getBodyAsText());

            EventContext ctx = null;
            if (doc != null) {
                DocumentEventContext docCtx = new DocumentEventContext(session, principal, doc);
                docCtx.setCategory(DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
                ctx = docCtx;
            } else {
                ctx = new EventContextImpl(session, principal);
            }
            ctx.setRepositoryName(documentLocation.getServerName());
            ctx.setProperties(properties);

            Event event = ctx.newEvent(eventId);
            Framework.getService(EventProducer.class).fireEvent(event);
            session.save();
        }
    }

}
