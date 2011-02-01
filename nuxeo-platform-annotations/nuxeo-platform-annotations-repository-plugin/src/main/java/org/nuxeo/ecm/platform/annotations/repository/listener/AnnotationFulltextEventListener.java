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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.platform.annotations.repository.service.AnnotatedDocumentEventListener;
import org.nuxeo.runtime.api.Framework;

/**
 * Extract the text of the body of the annotation to register it as a related
 * text resource on the document for full-text indexing by the repository.
 */
public class AnnotationFulltextEventListener implements EventListener {

    public static final String RELATED_TEXT_PROPERTY = "relatedtext";

    public static final String RELATED_TEXT_ID_PROPERTY = "relatedtextid";

    public static final String RELATED_TEXT_LIST_PROPERTY = "relatedtext:relatedtextresources";

    public static final String ANNOTATION_RESOURCE_ID_PREFIX = "annotation_";

    @Override
    public void handleEvent(Event event) throws ClientException {
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
            setAnnotationText(doc, annotationId, annotationBody);
        } else if (AnnotatedDocumentEventListener.ANNOTATION_DELETED.equals(event.getName())) {
            removeAnnotationText(doc, annotationId);
        } else if (AnnotatedDocumentEventListener.ANNOTATION_UPDATED.equals(event.getName())) {
            removeAnnotationText(doc, annotationId);
            setAnnotationText(doc, annotationId, annotationBody);
        } else {
            return;
        }
        session.saveDocument(doc);
    }

    protected void removeAnnotationText(DocumentModel doc, String annotationId)
            throws ClientException {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relatedResources = doc.getProperty(
                RELATED_TEXT_LIST_PROPERTY).getValue(List.class);
        String resourceId = makeResourceId(annotationId);
        Map<String, String> resourceToRemove = null;
        for (Map<String, String> resource : relatedResources) {
            if (resourceId.equals(resource.get(RELATED_TEXT_ID_PROPERTY))) {
                resourceToRemove = resource;
                break;
            }
        }
        if (resourceToRemove != null) {
            relatedResources.remove(resourceToRemove);
            doc.setPropertyValue(RELATED_TEXT_LIST_PROPERTY,
                    (Serializable) relatedResources);
        }
    }

    protected void setAnnotationText(DocumentModel doc, String annotationId,
            String annotationBody) throws ClientException {
        if (annotationBody == null) {
            return;
        }
        try {
            // strip HTML markup if any
            BlobHolder bh = new SimpleBlobHolder(new StringBlob(annotationBody,
                    "text/html"));
            ConversionService service = Framework.getService(ConversionService.class);
            if (service != null) {
                annotationBody = service.convert("html2text", bh, null).getBlob().getString();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relatedResources = doc.getProperty(
                RELATED_TEXT_LIST_PROPERTY).getValue(List.class);
        HashMap<String, String> resource = new HashMap<String, String>();
        resource.put(RELATED_TEXT_ID_PROPERTY, makeResourceId(annotationId));
        resource.put(RELATED_TEXT_PROPERTY, annotationBody);
        relatedResources.add(resource);
        doc.setPropertyValue(RELATED_TEXT_LIST_PROPERTY,
                (Serializable) relatedResources);
    }

    public static String makeResourceId(String annotationId) {
        return ANNOTATION_RESOURCE_ID_PREFIX + annotationId;
    }
}
