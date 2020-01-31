/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.common.utils.DateUtils.toCalendar;
import static org.nuxeo.common.utils.DateUtils.toInstant;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ANCESTOR_IDS_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_AUTHOR_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_CREATION_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_MODIFICATION_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_PROPERTY;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
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
    @Deprecated(since = "11.1")
    protected String id;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected String parentId;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected Collection<String> ancestorIds = new HashSet<>();

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected String author;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected String text;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected Instant creationDate;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected Instant modificationDate;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected String entityId;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
    protected String origin;

    /**
     * @deprecated since 11.1, not used due to {@link #docModel} usage
     */
    @Deprecated(since = "11.1")
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
    @Deprecated(since = "11.1")
    public void setId(String id) {
        // not used
    }

    @Override
    public String getParentId() {
        return (String) docModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY);
    }

    @Override
    public void setParentId(String parentId) {
        docModel.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, parentId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> getAncestorIds() {
        return (Collection<String>) docModel.getPropertyValue(COMMENT_ANCESTOR_IDS_PROPERTY);
    }

    @Override
    @Deprecated(since = "11.1")
    public void addAncestorId(String ancestorId) {
        // not used
    }

    @Override
    public String getAuthor() {
        return (String) docModel.getPropertyValue(COMMENT_AUTHOR_PROPERTY);
    }

    @Override
    public void setAuthor(String author) {
        docModel.setPropertyValue(COMMENT_AUTHOR_PROPERTY, author);
    }

    @Override
    public String getText() {
        return (String) docModel.getPropertyValue(COMMENT_TEXT_PROPERTY);
    }

    @Override
    public void setText(String text) {
        docModel.setPropertyValue(COMMENT_TEXT_PROPERTY, text);
    }

    @Override
    public Instant getCreationDate() {
        return toInstant((Calendar) docModel.getPropertyValue(COMMENT_CREATION_DATE_PROPERTY));
    }

    @Override
    public void setCreationDate(Instant creationDate) {
        docModel.setPropertyValue(COMMENT_CREATION_DATE_PROPERTY, toCalendar(creationDate));
    }

    @Override
    public Instant getModificationDate() {
        return toInstant((Calendar) docModel.getPropertyValue(COMMENT_MODIFICATION_DATE_PROPERTY));
    }

    @Override
    public void setModificationDate(Instant modificationDate) {
        docModel.setPropertyValue(COMMENT_MODIFICATION_DATE_PROPERTY, toCalendar(modificationDate));
    }

    @Override
    public String getEntityId() {
        if (docModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            return (String) docModel.getPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY);
        }
        return null;
    }

    @Override
    public void setEntityId(String entityId) {
        docModel.addFacet(EXTERNAL_ENTITY_FACET);
        docModel.setPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY, entityId);
    }

    @Override
    public String getOrigin() {
        if (docModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            return (String) docModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY);
        }
        return null;
    }

    @Override
    public void setOrigin(String origin) {
        docModel.addFacet(EXTERNAL_ENTITY_FACET);
        docModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY, origin);
    }

    @Override
    public String getEntity() {
        if (docModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            return (String) docModel.getPropertyValue(EXTERNAL_ENTITY_PROPERTY);
        }
        return null;
    }

    @Override
    public void setEntity(String entity) {
        docModel.addFacet(EXTERNAL_ENTITY_FACET);
        docModel.setPropertyValue(EXTERNAL_ENTITY_PROPERTY, entity);
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
}
