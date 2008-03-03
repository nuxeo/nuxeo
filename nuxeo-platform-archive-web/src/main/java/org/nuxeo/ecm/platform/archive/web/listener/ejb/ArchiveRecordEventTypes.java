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
 *     btatar
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.archive.web.listener.ejb;

/**
 * Constant utility class to specify archive records events,as create,edit or
 * delete
 *
 * @author btatar
 *
 */
public final class ArchiveRecordEventTypes {

    /**
     * This constant is used to specify that an event of archive record creation
     * has occured
     */
    public static final String ARCHIVE_RECORD_CREATED = "archiveRecordCreated";

    /**
     * This constant is used to specify that an event of archive record
     * modification has occured
     */
    public static final String ARCHIVE_RECORD_EDITED = "archiveRecordEdited";

    /**
     * This constant is used to specify that an event of archive record deletion
     * has occured
     */
    public static final String ARCHIVE_RECORD_DELETED = "archiveRecordDeleted";

    // Constant utility class
    private ArchiveRecordEventTypes() {
    }

}
