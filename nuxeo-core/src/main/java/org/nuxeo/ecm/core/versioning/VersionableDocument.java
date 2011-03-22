/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.versioning;

import java.util.Calendar;
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
     * @param checkinComment the checkin comment
     * @return the created version
     * @throws DocumentException
     */
    Document checkIn(String label, String checkinComment)
            throws DocumentException;

    void checkOut() throws DocumentException;

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
    List<Document> getVersions() throws DocumentException;

    /**
     * Gets the last version of this document.
     *
     * @return the last version
     * @throws DocumentException
     */
    Document getLastVersion() throws DocumentException;

    /**
     * Gets the head ("live") version of this document.
     *
     * @return
     * @throws DocumentException
     */
    Document getSourceDocument() throws DocumentException;

    /**
     * Replaces current version with version specified.
     *
     * @param version the version to replace with
     * @throws DocumentException
     */
    void restore(Document version) throws DocumentException;

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

    /**
     * Gets the version to which a checked in document is linked.
     * <p>
     * Returns {@code null} for a checked out document or a version or a proxy.
     *
     * @return the version, or {@code null}
     */
    Document getBaseVersion() throws DocumentException;

    boolean isCheckedOut() throws DocumentException;

    Calendar getVersionCreationDate() throws DocumentException;

    /**
     * Gets the version series id.
     *
     * @return the version series id
     */
    String getVersionSeriesId() throws DocumentException;

    String getVersionLabel() throws DocumentException;

    boolean isLatestVersion() throws DocumentException;

    boolean isMajorVersion() throws DocumentException;

    boolean isLatestMajorVersion() throws DocumentException;

    boolean isVersionSeriesCheckedOut() throws DocumentException;

    Document getWorkingCopy() throws DocumentException;

    String getCheckinComment() throws DocumentException;

}
