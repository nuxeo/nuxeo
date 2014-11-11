/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.annotations.repository.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotatedDocumentEventListener;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsFulltextInjector;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotationsRepositoryComponent;

/**
 * Extract the text of the body of the annotation to register it as a related
 * text resource on the document for full-text indexing by the repository.
 */
public class AnnotationFulltextEventListener implements EventListener {

    @Override
    public void handleEvent(Event event) throws ClientException {
        AnnotationsFulltextInjector injector = AnnotationsRepositoryComponent.instance.getFulltextInjector();
        if (!(event.getContext() instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext context = (DocumentEventContext) event.getContext();
        CoreSession session = context.getCoreSession();
        DocumentModel doc = context.getSourceDocument();
        if (doc == null) {
            // no need to update a deleted document
            return;
        }
        if (!doc.hasFacet(FacetNames.HAS_RELATED_TEXT)) {
            // no full-text indexing of annotation for this document type
            return;
        }
        String annotationId = (String) context.getProperty(AnnotatedDocumentEventListener.ANNOTATION_ID);
        String annotationBody = (String) context.getProperty(AnnotatedDocumentEventListener.ANNOTATION_BODY);

        if (AnnotatedDocumentEventListener.ANNOTATION_CREATED.equals(event.getName())) {
            injector.setAnnotationText(doc, annotationId, annotationBody);
            session.saveDocument(doc);
        } else if (AnnotatedDocumentEventListener.ANNOTATION_DELETED.equals(event.getName())) {
            if (injector.removeAnnotationText(doc, annotationId)) {
                session.saveDocument(doc);
            }
        } else if (AnnotatedDocumentEventListener.ANNOTATION_UPDATED.equals(event.getName())) {
            injector.removeAnnotationText(doc, annotationId);
            injector.setAnnotationText(doc, annotationId, annotationBody);
            session.saveDocument(doc);
        } else if (DocumentEventTypes.DOCUMENT_CHECKEDIN.equals(event.getName())) {
            // clean all annotation text before check-in: checked-in versions
            // handle their own annotations independently of the live version
            DocumentRef versionRef = (DocumentRef) context.getProperty("checkedInVersionRef");
            DocumentModel version = session.getDocument(versionRef);
            if (injector.removeAnnotationText(version, null)) {
                session.saveDocument(version);
            }
        } else {
            return;
        }
    }
}
