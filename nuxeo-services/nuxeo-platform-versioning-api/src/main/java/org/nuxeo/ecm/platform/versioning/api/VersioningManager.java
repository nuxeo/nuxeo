/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.versioning.api;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the Versioning Manager (a service).
 */
public interface VersioningManager {

    /**
     * Get document increment options as defined by versioning rules.
     *
     * @param doc the document
     * @return a list of version increment options available for the given document
     */
    VersionIncEditOptions getVersionIncEditOptions(DocumentModel doc);

    /**
     * Gets the label for the current version of a document, for the UI.
     *
     * @param doc the document
     * @return the version label
     */
    String getVersionLabel(DocumentModel doc);

    /**
     * Returns the property name to use when setting the major version for this document type.
     *
     * @deprecated since 5.4, use {@link DocumentModel#MAJOR_VERSION} directly
     */
    @Deprecated
    String getMajorVersionPropertyName(String documentType);

    /**
     * Returns the property name to use when setting the minor version for this document type.
     *
     * @deprecated since 5.4, use {@link DocumentModel#MINOR_VERSION} directly
     */
    @Deprecated
    String getMinorVersionPropertyName(String documentType);

    /**
     * Increments the minor version of a document.
     *
     * @param doc the document
     * @deprecated since 5.4
     */
    @Deprecated
    DocumentModel incrementMinor(DocumentModel doc);

    /**
     * Increments the major version of a document and sets the minor to 0.
     *
     * @param doc the document
     * @deprecated since 5.4
     */
    @Deprecated
    DocumentModel incrementMajor(DocumentModel doc);

}
