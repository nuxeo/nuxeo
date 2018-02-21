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
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CONTENT_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_CREATION_DATE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DATE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_DOCUMENT_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_INTERIOR_COLOR_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_TYPE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_FLAGS_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_LAST_MODIFIER_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_OPACITY_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_PAGE_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_POSITION_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SECURITY_PROPERTY;
import static org.nuxeo.ecm.annotation.AnnotationConstants.ANNOTATION_SUBJECT_PROPERTY;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 10.1
 */
public class AnnotationImpl implements Annotation {

    protected String id;

    protected String type;

    protected String color;

    protected String interiorColor;

    protected Calendar date;

    protected String flags;

    protected String documentId;

    protected String xpath;

    protected String lastModifier;

    protected long page;

    protected String position;

    protected Calendar creationDate;

    protected double opacity;

    protected String subject;

    protected String security;

    protected String content;

    protected String parentId;

    public AnnotationImpl() {
    }

    protected AnnotationImpl(DocumentModel annotationModel) {
        id = (String) annotationModel.getPropertyValue(ANNOTATION_ID_PROPERTY);
        type = (String) annotationModel.getPropertyValue(ANNOTATION_TYPE_PROPERTY);
        color = (String) annotationModel.getPropertyValue(ANNOTATION_COLOR_PROPERTY);
        interiorColor = (String) annotationModel.getPropertyValue(ANNOTATION_INTERIOR_COLOR_PROPERTY);
        date = (Calendar) annotationModel.getPropertyValue(ANNOTATION_DATE_PROPERTY);
        flags = (String) annotationModel.getPropertyValue(ANNOTATION_FLAGS_PROPERTY);
        documentId = (String) annotationModel.getPropertyValue(ANNOTATION_DOCUMENT_ID_PROPERTY);
        xpath = (String) annotationModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY);
        lastModifier = (String) annotationModel.getPropertyValue(ANNOTATION_LAST_MODIFIER_PROPERTY);
        page = annotationModel.getPropertyValue(ANNOTATION_PAGE_PROPERTY) != null
                ? (long) annotationModel.getPropertyValue(ANNOTATION_PAGE_PROPERTY) : 0L;
        position = (String) annotationModel.getPropertyValue(ANNOTATION_POSITION_PROPERTY);
        creationDate = (Calendar) annotationModel.getPropertyValue(ANNOTATION_CREATION_DATE_PROPERTY);
        opacity = annotationModel.getPropertyValue(ANNOTATION_OPACITY_PROPERTY) != null
                ? (double) annotationModel.getPropertyValue(ANNOTATION_OPACITY_PROPERTY) : 0d;
        subject = (String) annotationModel.getPropertyValue(ANNOTATION_SUBJECT_PROPERTY);
        security = (String) annotationModel.getPropertyValue(ANNOTATION_SECURITY_PROPERTY);
        content = (String) annotationModel.getPropertyValue(ANNOTATION_CONTENT_PROPERTY);
        parentId = (String) annotationModel.getPropertyValue(ANNOTATION_PARENT_ID_PROPERTY);
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
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String getInteriorColor() {
        return interiorColor;
    }

    @Override
    public void setInteriorColor(String color) {
        interiorColor = color;
    }

    @Override
    public Calendar getDate() {
        return date;
    }

    @Override
    public void setDate(Calendar date) {
        this.date = date;
    }

    @Override
    public String getFlags() {
        return flags;
    }

    @Override
    public void setFlags(String flags) {
        this.flags = flags;
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
    public String getLastModifier() {
        return lastModifier;
    }

    @Override
    public void setLastModifier(String lastModifier) {
        this.lastModifier = lastModifier;
    }

    @Override
    public long getPage() {
        return page;
    }

    @Override
    public void setPage(long page) {
        this.page = page;
    }

    @Override
    public String getPosition() {
        return position;
    }

    @Override
    public void setPosition(String position) {
        this.position = position;
    }

    @Override
    public Calendar getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public double getOpacity() {
        return opacity;
    }

    @Override
    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getSecurity() {
        return security;
    }

    @Override
    public void setSecurity(String security) {
        this.security = security;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

}
