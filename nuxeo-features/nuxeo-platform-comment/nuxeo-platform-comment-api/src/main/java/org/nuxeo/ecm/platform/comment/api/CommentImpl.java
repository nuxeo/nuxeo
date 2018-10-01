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

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;

/**
 * @since 10.3
 */
public class CommentImpl implements Comment, ExternalEntity {

    protected String id;

    protected String parentId;

    protected Collection<String> ancestorIds = new HashSet<>();

    protected String author;

    protected String text;

    protected Instant creationDate;

    protected Instant modificationDate;

    protected String entityId;

    protected String origin;

    protected String entity;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public Collection<String> getAncestorIds() {
        return ancestorIds;
    }

    @Override
    public void addAncestorId(String ancestorId) {
        ancestorIds.add(ancestorId);
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Instant getCreationDate() {
        return creationDate;
    }

    @Override
    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public Instant getModificationDate() {
        return modificationDate;
    }

    @Override
    public void setModificationDate(Instant modificationDate) {
        this.modificationDate = modificationDate;
    }

    @Override
    public String getEntityId() {
        return entityId;
    }

    @Override
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(String origin) {
        this.origin = origin;
    }

    @Override
    public String getEntity() {
        return entity;
    }

    @Override
    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, ancestorIds, author, text, creationDate, modificationDate, entityId, origin,
                entity);
    }
}
