/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning.api;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for the Versioning Manager (a service).
 */
public interface VersioningManager {

    /**
     * Get document increment options as defined by versioning rules.
     *
     * @param doc the document
     * @return a list of version increment options available for the given
     *         document
     */
    VersionIncEditOptions getVersionIncEditOptions(DocumentModel doc)
            throws ClientException;

    /**
     * Gets the label for the current version of a document, for the UI.
     *
     * @param doc the document
     * @return the version label
     */
    String getVersionLabel(DocumentModel doc) throws ClientException;

    /**
     * Returns the property name to use when setting the major version for this
     * document type.
     *
     * @deprecated since 5.4, use {@link DocumentModel#MAJOR_VERSION} directly
     */
    @Deprecated
    String getMajorVersionPropertyName(String documentType);

    /**
     * Returns the property name to use when setting the minor version for this
     * document type.
     *
     * @deprecated since 5.4, use {@link DocumentModel#MINOR_VERSION} directly
     */
    @Deprecated
    String getMinorVersionPropertyName(String documentType);

    /**
     * Increments the minor version of a document.
     *
     * @param doc the document
     *
     * @deprecated since 5.4
     */
    @Deprecated
    DocumentModel incrementMinor(DocumentModel doc) throws ClientException;

    /**
     * Increments the major version of a document and sets the minor to 0.
     *
     * @param doc the document
     *
     * @deprecated since 5.4
     */
    @Deprecated
    DocumentModel incrementMajor(DocumentModel doc) throws ClientException;

}
