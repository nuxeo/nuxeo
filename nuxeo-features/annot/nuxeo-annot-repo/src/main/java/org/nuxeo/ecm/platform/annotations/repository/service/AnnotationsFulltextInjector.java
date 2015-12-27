/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.platform.annotations.repository.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 */
public class AnnotationsFulltextInjector {

    public static final String RELATED_TEXT_PROPERTY = "relatedtext";

    public static final String RELATED_TEXT_ID_PROPERTY = "relatedtextid";

    public static final String RELATED_TEXT_LIST_PROPERTY = "relatedtext:relatedtextresources";

    public static final String ANNOTATION_RESOURCE_ID_PREFIX = "annotation_";

    public boolean removeAnnotationText(DocumentModel doc, String annotationId) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relatedResources = doc.getProperty(RELATED_TEXT_LIST_PROPERTY).getValue(List.class);
        String resourceIdToRemove = annotationId == null ? null : makeResourceId(annotationId);
        List<Map<String, String>> resourcesToRemove = new ArrayList<Map<String, String>>();
        for (Map<String, String> resource : relatedResources) {
            String resourceId = resource.get(RELATED_TEXT_ID_PROPERTY);
            if (resourceIdToRemove != null) {
                if (resourceIdToRemove.equals(resourceId)) {
                    resourcesToRemove.add(resource);
                }
            } else {
                // remove all annotations
                if (resourceId == null || resourceId.startsWith(ANNOTATION_RESOURCE_ID_PREFIX)) {
                    resourcesToRemove.add(resource);
                }
            }
        }
        if (!resourcesToRemove.isEmpty()) {
            relatedResources.removeAll(resourcesToRemove);
            doc.setPropertyValue(RELATED_TEXT_LIST_PROPERTY, (Serializable) relatedResources);
            return true;
        }
        return false;
    }

    public void setAnnotationText(DocumentModel doc, String annotationId, String annotationBody) {
        if (annotationBody == null) {
            return;
        }
        // strip HTML markup if any
        BlobHolder bh = new SimpleBlobHolder(Blobs.createBlob(annotationBody, "text/html"));
        ConversionService service = Framework.getService(ConversionService.class);
        if (service != null) {
            try {
                annotationBody = service.convert("html2text", bh, null).getBlob().getString();
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        }
        @SuppressWarnings("unchecked")
        List<Map<String, String>> relatedResources = doc.getProperty(RELATED_TEXT_LIST_PROPERTY).getValue(List.class);
        HashMap<String, String> resource = new HashMap<String, String>();
        resource.put(RELATED_TEXT_ID_PROPERTY, makeResourceId(annotationId));
        resource.put(RELATED_TEXT_PROPERTY, annotationBody);
        relatedResources.add(resource);
        doc.setPropertyValue(RELATED_TEXT_LIST_PROPERTY, (Serializable) relatedResources);
    }

    protected static String makeResourceId(String annotationId) {
        return ANNOTATION_RESOURCE_ID_PREFIX + annotationId;
    }

}
