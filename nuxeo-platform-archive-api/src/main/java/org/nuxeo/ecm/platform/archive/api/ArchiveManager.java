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
 * $Id: LogEntry.java 1362 2006-07-26 14:26:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.archive.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Archives manager interface.
 * <p>
 * :XXX: http://jira.nuxeo.org/browse/NXP-514
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
public interface ArchiveManager extends Serializable {

    /**
     * Returns the archive records of a document by giving the doc uid.
     * <p>
     *
     * @param docUID the document UID
     * @return a list of archive records attached to the document
     * @throws Exception
     */
    List<ArchiveRecord> getArchiveRecordsByDocUID(String docUID)
            throws Exception;

    /**
     * Returns the archive record object with the specified id.
     *
     * @param archiveRecordId the id of the archive record
     * @return the archive record with the specified id
     * @throws Exception
     */
    ArchiveRecord findArchiveRecordById(long archiveRecordId) throws Exception;

    /**
     * Returns the archive records matched by the specified JPA query.
     * The parameters map provides the named parameters and values to be set on the query.
     *
     * @param qlString the query to execute
     * @param parameters the named parameters to be set on the query
     * @return
     */
    List<ArchiveRecord> findArchiveRecords(String qlString, Map<String, Object>  parameters);

    /**
     *
     * @return
     * @throws Exception
     */
    ArchiveRecord searchArchiveRecord() throws Exception;

    /**
     *
     * @param record the archive record info to be added
     * @throws Exception
     */
    void addArchiveRecord(ArchiveRecord record) throws Exception;

    /**
     *
     * @param record the archive record to edit
     * @throws Exception
     */
    void editArchiveRecord(ArchiveRecord record) throws Exception;

    /**
     *
     * @param recordId id for the archive record to delete
     * @throws Exception
     */
    Boolean deleteArchiveRecord(long recordId) throws Exception;

}
