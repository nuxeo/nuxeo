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


/**
 * Annotation interface
 * 
 * @since 10.1
 */
public interface Annotation {

    /**
     * Gets annotation id.
     * 
     * @return the id
     */
    String getId();

    /**
     * Sets annotation id.
     * 
     * @param id the id
     */
    void setId(String id);

    /**
     * Gets the annotated document id.
     *
     * @return the annotated document id
     */
    String getDocumentId();

    /**
     * Sets the annotated document id.
     *
     * @param documentId the annotated document id
     */
    void setDocumentId(String documentId);

    /**
     * Gets the xpath of annotated blob in the document.
     *
     * @return the xpath
     */
    String getXpath();

    /**
     * Sets the xpath of annotated blob in the document.
     *
     * @param xpath the xpath
     */
    void setXpath(String xpath);

    /**
     * Gets annotation entity. This represents the annotation model as the rendition server describes it.
     * 
     * @return the entity
     */
    String getEntity();

    /**
     * Sets annotation entity.
     * 
     * @param entity the entity
     */
    void setEntity(String entity);

}
