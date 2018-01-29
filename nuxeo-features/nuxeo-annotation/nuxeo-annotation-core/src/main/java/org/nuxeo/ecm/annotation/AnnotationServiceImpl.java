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

import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_COLOR_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CREATION_DATE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DATE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_FLAGS_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_LAST_MODIFIER_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_NAME_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_OPACITY_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PAGE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_POSITION_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SECURITY_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SUBJECT_PROPERTY;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 10.1
 */
public class AnnotationServiceImpl extends DefaultComponent implements AnnotationService {

    private static final Log log = LogFactory.getLog(AnnotationServiceImpl.class);

    protected static final String ANNOTATION_NAME = "annotation";

    protected static final String ANNOTATION_PAGEPROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT";

    @Override
    public Annotation createAnnotation(CoreSession session, String documentId, String xpath,
            Annotation annotation) {

        // Create annotation as a placeless document
        DocumentModel annotationModel = session.createDocumentModel(null, ANNOTATION_NAME, ANNOTATION_DOC_TYPE);
        annotationModel.setPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY, documentId);
        annotationModel.setPropertyValue(ANNOTATION_XPATH_PROPERTY, xpath);
        setAnnotationProperties(annotationModel, annotation);
        annotationModel = session.createDocument(annotationModel);
        return new AnnotationImpl(annotationModel);
    }

    @Override
    public Annotation getAnnotation(CoreSession session, String documentId, String xpath, String annotationId) {
        DocumentModel annotationModel = session.getDocument(new IdRef(annotationId));
        return new AnnotationImpl(annotationModel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Annotation> getAnnotations(CoreSession session, String documentId, String xpath) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = Collections.singletonMap(CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        List<DocumentModel> annotationList = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                ANNOTATION_PAGEPROVIDER_NAME, null, null, null, props, documentId, xpath)).getCurrentPage();
        return annotationList.stream().map(AnnotationImpl::new).collect(Collectors.toList());
    }

    @Override
    public void updateAnnotation(CoreSession session, String documentId, String xpath, Annotation annotation) {
        String annotationId = annotation.getId();
        IdRef annotationRef = new IdRef(annotationId);
        if (!session.exists(annotationRef)) {
            if (log.isWarnEnabled()) {
                log.warn("The annotation " + annotationId + " on document " + xpath
                        + " does not exist. Update operation is ignored.");
            }
            return;
        }
        DocumentModel annotationModel = session.getDocument(annotationRef);
        setAnnotationProperties(annotationModel, annotation);
        session.saveDocument(annotationModel);
    }

    @Override
    public void deleteAnnotation(CoreSession session, String documentId, String xpath, String annotationId)
            throws IllegalArgumentException {
        IdRef annotationRef = new IdRef(annotationId);
        if (!session.exists(annotationRef)) {
            throw new IllegalArgumentException(
                    "The annotation " + annotationId + " on the document " + xpath + " does not exist.");
        }
        session.removeDocument(annotationRef);
    }

    protected void setAnnotationProperties(DocumentModel annotationModel, Annotation annotation) {
        annotationModel.setPropertyValue(ANNOTATION_COLOR_PROPERTY, annotation.getColor());
        annotationModel.setPropertyValue(ANNOTATION_DATE_PROPERTY, annotation.getDate());
        annotationModel.setPropertyValue(ANNOTATION_FLAGS_PROPERTY, annotation.getFlags());
        annotationModel.setPropertyValue(ANNOTATION_NAME_PROPERTY, annotation.getName());
        annotationModel.setPropertyValue(ANNOTATION_LAST_MODIFIER_PROPERTY, annotation.getLastModifier());
        annotationModel.setPropertyValue(ANNOTATION_PAGE_PROPERTY, annotation.getPage());
        annotationModel.setPropertyValue(ANNOTATION_POSITION_PROPERTY, annotation.getPosition());
        annotationModel.setPropertyValue(ANNOTATION_CREATION_DATE_PROPERTY, annotation.getCreationDate());
        annotationModel.setPropertyValue(ANNOTATION_OPACITY_PROPERTY, annotation.getOpacity());
        annotationModel.setPropertyValue(ANNOTATION_SUBJECT_PROPERTY, annotation.getSubject());
        annotationModel.setPropertyValue(ANNOTATION_SECURITY_PROPERTY, annotation.getSecurity());
    }

}
