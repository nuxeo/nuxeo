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

package org.nuxeo.ecm.core.api.facet;

import org.nuxeo.ecm.core.api.DocumentException;

/**
 * Declares constants and methods used to control document versions mostly when
 * a document is saved.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface VersioningDocument {

    // TODO make a method shouldCreateSnapshotOnSave()...
    // that looks into contextData of this object and
    // reads the boolean with this key
    // - make changes in AbstractSession afterwards

    String CREATE_SNAPSHOT_ON_SAVE_KEY = "CREATE_SNAPSHOT_ON_SAVE";

    /**
     * Key passed in options to event to inform it that the document was just
     * snapshotted. Used to avoid incrementing versions twice.
     */
    String DOCUMENT_WAS_SNAPSHOTTED = "DOCUMENT_WAS_SNAPSHOTTED";

    /**
     * Key used in options map to send current versions to versioning listener
     * so it will know what version the document had before restoring.
     */
    String CURRENT_DOCUMENT_MINOR_VERSION_KEY = "CURRENT_DOCUMENT_MINOR_VERSION";

    String CURRENT_DOCUMENT_MAJOR_VERSION_KEY = "CURRENT_DOCUMENT_MAJOR_VERSION";

    /**
     * Key used in options map to send the UUID of the version being restored to
     * the listeners
     */
    String RESTORED_VERSION_UUID_KEY = "RESTORED_VERSION_UUID";

    Long getMinorVersion() throws DocumentException;

    void setMinorVersion(Long value);

    Long getMajorVersion() throws DocumentException;

    void setMajorVersion(Long value);

    /**
     * This will force the adapter to re-load document from repository. Useful
     * when versioning data is broken.
     *
     * @throws DocumentException
     */
    void refetchDoc() throws DocumentException;

    /**
     * Increments major, minor version fields according to the existing rules.
     */
    void incrementVersions();

    void incrementMajor() throws DocumentException;

    void incrementMinor() throws DocumentException;

    /**
     * Creates a string from minor and major formatted with specified number of
     * digits.
     *
     * @param majorDigits
     * @param minorDigits
     * @param sep
     * @return
     */
    String getVersionAsString(int majorDigits, int minorDigits, char sep)
            throws DocumentException;

}
