/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.annotation;

import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ENTITY_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @since 10.1
 */
public class AnnotationServiceImpl extends DefaultComponent implements AnnotationService {

    private static final Log log = LogFactory.getLog(AnnotationServiceImpl.class);

    protected static final String ANNOTATION_NAME = "annotation";

    protected static final String GET_ANNOTATION_PAGEPROVIDER_NAME = "GET_ANNOTATION";

    protected static final String GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT";

    protected static final String ANNOTATIONS_PLACELESS_STORAGE_PROPERTY = "nuxeo.annotations.placeless.storage";

    protected static final String HIDDEN_FOLDER_TYPE = "HiddenFolder";

    protected static final String ANNOTATION_FOLDER_NAME = "Annotations";

    @Override
    public Annotation createAnnotation(CoreSession session, Annotation annotation) {

        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        boolean annotationPlaceless = configurationService.isBooleanPropertyTrue(
                ANNOTATIONS_PLACELESS_STORAGE_PROPERTY);

        return CoreInstance.doPrivileged(session, s -> {
            String path = null;
            if (!annotationPlaceless) {
                // Create or retrieve the folder to store the annotation.
                // If the document is under a domain, the folder is a child of this domain.
                // Otherwise, it is a child of the root document.
                DocumentModel annotatedDoc = s.getDocument(new IdRef(annotation.getDocumentId()));
                String parentPath = s.getRootDocument().getPathAsString();
                if (annotatedDoc.getPath().segmentCount() > 1) {
                    parentPath += annotatedDoc.getPath().segment(0);
                }
                PathRef ref = new PathRef(parentPath, ANNOTATION_FOLDER_NAME);
                DocumentModel annotationFolderDoc = s.createDocumentModel(parentPath, ANNOTATION_FOLDER_NAME,
                        HIDDEN_FOLDER_TYPE);
                s.getOrCreateDocument(annotationFolderDoc);
                s.save();
                path = ref.toString();
            }
            DocumentModel annotationModel = s.createDocumentModel(path, ANNOTATION_NAME, ANNOTATION_DOC_TYPE);
            setAnnotationProperties(annotationModel, annotation);
            annotationModel = s.createDocument(annotationModel);
            return new AnnotationImpl(annotationModel);
        });
    }

    @Override
    public Annotation getAnnotation(CoreSession session, String annotationId) {
        return CoreInstance.doPrivileged(session, s -> {
            DocumentModel annotationModel = getAnnotationModel(s, annotationId);
            if (annotationModel == null) {
                return null;
            }
            return new AnnotationImpl(annotationModel);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Annotation> getAnnotations(CoreSession session, String documentId, String xpath) {
        return CoreInstance.doPrivileged(session, s -> {
            PageProviderService ppService = Framework.getService(PageProviderService.class);
            Map<String, Serializable> props = Collections.singletonMap(
                    CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY, (Serializable) s);
            List<DocumentModel> annotationList = //
                    ((PageProvider<DocumentModel>) ppService.getPageProvider(GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME,
                            null, null, null, props, documentId, xpath)).getCurrentPage();
            return annotationList.stream().map(AnnotationImpl::new).collect(Collectors.toList());
        });
    }

    @Override
    public void updateAnnotation(CoreSession session, Annotation annotation) {
        CoreInstance.doPrivileged(session, s -> {
            DocumentModel annotationModel = getAnnotationModel(s, annotation.getId());
            if (annotationModel == null) {
                if (log.isWarnEnabled()) {
                    log.warn("The annotation " + annotation.getId() + " on document blob " + annotation.getXpath()
                            + " does not exist. Update operation is ignored.");
                }
                return;
            }
            setAnnotationProperties(annotationModel, annotation);
            s.saveDocument(annotationModel);
        });
    }

    @Override
    public void deleteAnnotation(CoreSession session, String annotationId) throws IllegalArgumentException {
        CoreInstance.doPrivileged(session, s -> {
            DocumentModel annotationModel = getAnnotationModel(s, annotationId);
            if (annotationModel == null) {
                throw new IllegalArgumentException("The annotation " + annotationId + " does not exist.");
            }
            s.removeDocument(annotationModel.getRef());
        });
    }

    protected void setAnnotationProperties(DocumentModel annotationModel, Annotation annotation) {
        annotationModel.setPropertyValue(ANNOTATION_ID_PROPERTY, annotation.getId());
        annotationModel.setPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY, annotation.getDocumentId());
        annotationModel.setPropertyValue(ANNOTATION_XPATH_PROPERTY, annotation.getXpath());
        annotationModel.setPropertyValue(ANNOTATION_ENTITY_PROPERTY, annotation.getEntity());
    }

    /**
     * Session must be privileged.
     */
    @SuppressWarnings("unchecked")
    protected DocumentModel getAnnotationModel(CoreSession session, String annotationId) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = Collections.singletonMap(CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        List<DocumentModel> results = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_ANNOTATION_PAGEPROVIDER_NAME, null, null, null, props, annotationId)).getCurrentPage();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

}
