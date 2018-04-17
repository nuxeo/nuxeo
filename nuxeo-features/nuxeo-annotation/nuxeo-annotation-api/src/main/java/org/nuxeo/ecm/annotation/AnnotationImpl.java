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
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ENTITY_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 10.1
 */
public class AnnotationImpl implements Annotation {

    protected String id;

    protected String documentId;

    protected String xpath;

    protected String entity;

    public AnnotationImpl() {
    }

    protected AnnotationImpl(DocumentModel annotationModel) {
        id = (String) annotationModel.getPropertyValue(ANNOTATION_ID_PROPERTY);
        documentId = (String) annotationModel.getPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY);
        xpath = (String) annotationModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY);
        entity = (String) annotationModel.getPropertyValue(ANNOTATION_ENTITY_PROPERTY);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getDocumentId() {
        return documentId;
    }

    @Override
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    @Override
    public String getXpath() {
        return xpath;
    }

    @Override
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    @Override
    public String getEntity() {
        return entity;
    }

    @Override
    public void setEntity(String entity) {
        this.entity = entity;
    }

}
