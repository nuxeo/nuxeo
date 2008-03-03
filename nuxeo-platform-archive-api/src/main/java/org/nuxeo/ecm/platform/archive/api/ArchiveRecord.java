/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.archive.api;

import java.io.Serializable;
import java.util.Date;

/**
 * Archive record interface.
 * <p>
 *
 * @author <a href="mailto:bt@nuxeo.com">Bogdan Tatar</a>
 */
public interface ArchiveRecord extends Serializable {

    /**
     * Returns the archive record identifier.
     *
     * @return the archive record identifier
     */
    long getId();

    /**
     * Returns the UID of the document related to the archive record.
     *
     * @return the UID of the document
     */
    String getDocUID();

    /**
     * Return the version of the document related to the archive record.
     *
     * @return the version of the document
     */
    String getDocVersion();

    /**
     * Return the life cycle of the document related to the archive record.
     *
     * @return the life cycle of the document
     */
    String getDocLifeCycle();

    /**
     * Return the title of the document related to the archive record.
     *
     * @return the title of the document
     */
    String getDocTitle();

    /**
     * Return the path of the document related to the archive record.
     *
     * @return the path of the document
     */
    String getParentDocPath();

    /**
     * Return the retention support of an archive record.
     *
     * @return the retention support of an archive record
     */
    String getRetentionMediumState();

    /**
     *
     * @return
     */
    String getRetentionMediumQuality();

    /**
     *
     * @return
     */
    String getRetentionMediumType();

    /**
     *
     * @return
     */
    String getRetentionMediumLocation();

    /**
     * Returns the date when the archive record was created.
     *
     * @return the date when the archive record was created
     */
    Date getArchiveDate();

    /**
     * Return format1 of the archive record.
     *
     * @return format1 of the archive record
     */
    String getFormat1();

    /**
     *
     * @return
     */
    Integer getFolios1();

    /**
     * Return the number of microform for the archive record.
     *
     * @return the number of microform
     */
    Integer getMicroformNumber();

    /**
     *
     * @return
     */
    String getRestMediumState();

    /**
     *
     * @return
     */
    String getRestMediumType();

    /**
     *
     * @return
     */
    String getRestMediumLoc();

    /**
     *
     * @return
     */
    String getWritableMediumLoc();

    /**
     *
     * @return
     */
    String getWritableMediumType();

    /**
     *
     * @return
     */
    String getWritableMediumState();

    /**
     *
     * @return
     */
    Integer getRetentionMediumMaxAge();

    /**
     *
     * @return
     */
    Date getRetentionMediumLastYear();

    /**
     * Returns the duration of the transition between the valid and expiration
     * stages plus de duration of conservation.
     *
     * @return
     */
    Date getEstimatedRemovalDate();

    /**
     * Returns the gathering folder.
     *
     * @return the gathering folder
     */
    String getGatheringFolder();
}
