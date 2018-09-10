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
 */

package org.nuxeo.ecm.platform.comment.api;

import java.time.Instant;
import java.util.Collection;

/**
 * Comment interface.
 * 
 * @since 10.3
 */
public interface Comment {

    /**
     * Gets comment id.
     *
     * @return the id
     */
    String getId();

    /**
     * Sets comment id.
     *
     * @param id the id
     */
    void setId(String id);

    /**
     * Gets parent id.
     *
     * @return the parent id
     */
    String getParentId();

    /**
     * Sets parent id.
     *
     * @param parentId the parent id
     */
    void setParentId(String parentId);

    /**
     * Gets the list of ancestor ids.
     * 
     * @return the list of ancestor ids
     */
    Collection<String> getAncestorIds();

    /**
     * Adds an ancestor id.
     * 
     * @param ancestorId the ancestor id
     */
    void addAncestorId(String ancestorId);

    /**
     * Gets comment author.
     * 
     * @return the author
     */
    String getAuthor();

    /**
     * Sets comment author.
     * 
     * @param author the author
     */
    void setAuthor(String author);

    /**
     * Gets comment text.
     * 
     * @return the text
     */
    String getText();

    /**
     * Sets comment text.
     * 
     * @param text the text
     */
    void setText(String text);

    /**
     * Gets comment creation date.
     * 
     * @return the creation date
     */
    Instant getCreationDate();

    /**
     * Sets comment creation date.
     * 
     * @param creationDate the creation date
     */
    void setCreationDate(Instant creationDate);

    /**
     * Sets comment modification date.
     * 
     * @return the modification date
     */
    Instant getModificationDate();

    /**
     * Sets comment modification date.
     * 
     * @param modificationDate the modification date
     */
    void setModificationDate(Instant modificationDate);
}
