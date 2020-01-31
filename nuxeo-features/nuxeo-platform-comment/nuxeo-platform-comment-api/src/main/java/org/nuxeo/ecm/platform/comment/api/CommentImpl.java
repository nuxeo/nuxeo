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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.api;

import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;

/**
 * @since 10.3
 */
public class CommentImpl implements Comment, ExternalEntity {

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String id;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String parentId;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected Collection<String> ancestorIds = new HashSet<>();

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String author;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String text;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected Instant creationDate;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected Instant modificationDate;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String entityId;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String origin;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated
    protected String entity;

    /**
     * {@link DocumentModel} storing the {@link Comment} metadata.
     * 
     * @since 11.1
     */
    protected DocumentModel docModel;

    /**
     * @since 11.1
     */
    public CommentImpl() {
        this(SimpleDocumentModel.ofType(COMMENT_DOC_TYPE));
    }

    /**
     * Constructor for the document adapter factory.
     *
     * @since 11.1
     */
    protected CommentImpl(DocumentModel docModel) {
        this.docModel = docModel;
        this.docModel.detach(true);
    }

    @Override
    public String getId() {
        try {
            return docModel.getId();
        } catch (UnsupportedOperationException e) {
            // don't fail when docModel is SimpleDocumentModel
            return null;
        }
    }

    @Override
    @Deprecated
    public void setId(String id) {
        // not used
    }

    @Override
    public String getParentId() {
        return (String) docModel.getPropertyValue(COMMENT_PARENT_ID);
    }

    @Override
    public void setParentId(String parentId) {
        docModel.setPropertyValue(COMMENT_PARENT_ID, parentId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getAncestorIds() {
        return (Collection<String>) docModel.getPropertyValue(COMMENT_ANCESTOR_IDS);
    }

    @Override
    @Deprecated
    public void addAncestorId(String ancestorId) {
        // not used
    }

    @Override
    public String getAuthor() {
        return (String) docModel.getPropertyValue(COMMENT_AUTHOR);
    }

    @Override
    public void setAuthor(String author) {
        docModel.setPropertyValue(COMMENT_AUTHOR, author);
    }

    @Override
    public String getText() {
        return (String) docModel.getPropertyValue(COMMENT_TEXT);
    }

    @Override
    public void setText(String text) {
        docModel.setPropertyValue(COMMENT_TEXT, text);
    }

    @Override
    public Instant getCreationDate() {
        Calendar cal = (Calendar) docModel.getPropertyValue(COMMENT_CREATION_DATE);
        return cal == null ? null : cal.toInstant();
    }

    @Override
    public void setCreationDate(Instant creationDate) {
        docModel.setPropertyValue(COMMENT_CREATION_DATE, toCalendar(creationDate));
    }

    @Override
    public Instant getModificationDate() {
        Calendar cal = (Calendar) docModel.getPropertyValue(COMMENT_MODIFICATION_DATE);
        return cal == null ? null : cal.toInstant();
    }

    @Override
    public void setModificationDate(Instant modificationDate) {
        docModel.setPropertyValue(COMMENT_MODIFICATION_DATE, toCalendar(modificationDate));
    }

    @Override
    public String getEntityId() {
        if (docModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            return (String) docModel.getPropertyValue(EXTERNAL_ENTITY_ID);
        }
        return null;
    }

    @Override
    public void setEntityId(String entityId) {
        docModel.addFacet(EXTERNAL_ENTITY_FACET);
        docModel.setPropertyValue(EXTERNAL_ENTITY_ID, entityId);
    }

    @Override
    public String getOrigin() {
        if (docModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            return (String) docModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN);
        }
        return null;
    }

    @Override
    public void setOrigin(String origin) {
        docModel.addFacet(EXTERNAL_ENTITY_FACET);
        docModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN, origin);
    }

    @Override
    public String getEntity() {
        if (docModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            return (String) docModel.getPropertyValue(EXTERNAL_ENTITY);
        }
        return null;
    }

    @Override
    public void setEntity(String entity) {
        docModel.addFacet(EXTERNAL_ENTITY_FACET);
        docModel.setPropertyValue(EXTERNAL_ENTITY, entity);
    }

    @Override
    public DocumentModel getDocument() {
        return docModel;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return docModel.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", getId()).toString();
    }

    protected static Calendar toCalendar(Instant instant) {
        if (instant == null) {
            return null;
        }
        // an Instant is on UTC by definition
        ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
        return GregorianCalendar.from(zdt);
    }
}
