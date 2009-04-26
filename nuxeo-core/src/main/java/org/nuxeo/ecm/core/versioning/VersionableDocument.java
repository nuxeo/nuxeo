/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.versioning;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Document;

/**
 * @author <a href="eionica@nuxeo.com">Eugen Ionica</a>
 *
 */

public interface VersionableDocument {

    /**
     * Creates a new version.
     *
     * @param label the version label
     * @throws DocumentException
     */
    void checkIn(String label) throws DocumentException;

    /**
     * Creates a new version.
     *
     * @param label the version label
     * @param description the version description
     * @throws DocumentException
     */
    void checkIn(String label, String description)
            throws DocumentException;

    void checkOut() throws DocumentException;

    boolean isCheckedOut() throws DocumentException;

    /**
     * Gets the list of version ids for this document.
     *
     * @return the list of version ids
     * @throws DocumentException
     * @since 1.4.1
     */
    List<String> getVersionsIds() throws DocumentException;

    /**
     * @return all versions of the document, empty list if there's no version
     * @throws DocumentException
     */
    DocumentVersionIterator getVersions() throws DocumentException;

    /**
     * Gets the last version of this document.
     *
     * @return the last version
     * @throws DocumentException
     */
    DocumentVersion getLastVersion() throws DocumentException;


    /**
     * Gets the head ("live") version of this document.
     *
     * @return
     * @throws DocumentException
     */
    Document getSourceDocument() throws DocumentException;

    /**
     * Replaces current version with version specified by given label.
     *
     * @param label the version label
     * @throws DocumentException
     */
    void restore(String label) throws DocumentException;

    /**
     * Gets a version of this document, given its label.
     *
     * @param label the version label
     * @return the version
     * @throws DocumentException
     */
    Document getVersion(String label) throws DocumentException;

    /**
     * Tells the caller if this document has versions or not.
     * <p>
     * Needed to know if we need to use checkin/checkout methods when changing a
     * document.
     *
     * @return true if there are versions
     * @throws DocumentException
     */
    boolean hasVersions() throws DocumentException;

    /**
     * Checks whether or not this doc is a version document.
     *
     * @return true if it's a version, false otherwise
     * @return
     */
    boolean isVersion();

}
