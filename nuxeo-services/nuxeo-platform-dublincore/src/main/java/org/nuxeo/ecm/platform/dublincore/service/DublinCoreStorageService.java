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
 *     Nuno Cunha (ncunha@nuxeo.com)
 */

package org.nuxeo.ecm.platform.dublincore.service;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;

/**
 * Former implementation for DublinCore schema storage.
 *
 * @since 10.2
 */
public interface DublinCoreStorageService {

    String ID = "DublinCoreStorageService";

    /**
     * Sets the document's creation date.
     */
    void setCreationDate(DocumentModel doc, Calendar creationDate);

    /**
     * Sets the document's creation date.
     *
     * @deprecated since 10.2, use directly {@link DublinCoreStorageService#setCreationDate(DocumentModel, Calendar)}
     */
    @Deprecated
    void setCreationDate(DocumentModel doc, Calendar creationDate, Event event);

    /**
     * Sets the document's issued date.
     */
    void setIssuedDate(DocumentModel doc, Calendar issuedDate);

    /**
     * Sets the document's modified date.
     */
    void setModificationDate(DocumentModel doc, Calendar modificationDate);

    /**
     * Sets the document's modified date.
     *
     * @deprecated since 10.2, use directly
     *             {@link DublinCoreStorageService#setModificationDate(DocumentModel, Calendar)}
     */
    @Deprecated
    void setModificationDate(DocumentModel doc, Calendar modificationDate, Event event);

    /**
     * Adds a contributor to the document.
     */
    void addContributor(DocumentModel doc, Event event);

}
