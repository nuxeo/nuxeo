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
 * $Id: LogEntry.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;
import java.util.Date;

/**
 * Log entry interface.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface LogEntry extends Serializable {

    /**
     * Returns the log identifier.
     *
     * @return the log identifier
     */
    long getId();

    /**
     * Returns the name of the principal who originated the log entry.
     *
     * @return the name of the principal who originated the log entry
     */
    String getPrincipalName();

    /**
     * Returns the identifier of the event that originated the log entry.
     *
     * @return the identifier of the event that originated the log entry
     */
    String getEventId();

    /**
     * Returns the date of the event that originated the log entry.
     *
     * @return the date of the event that originated the log entry
     */
    Date getEventDate();

    /**
     * Returns the category for this log entry.
     * <p>
     * This is defined at client level. Categories are not restricted in any
     * ways.
     *
     * @return the category for this log entry.
     */
    String getCategory();

    /**
     * Returns the associated comment for this log entry.
     *
     * @return the associated comment for this log entry
     */
    String getComment();

    /**
     * Returns the doc UUID related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the doc UUID related to the log entry.
     */
    String getDocUUID();

    /**
     * Returns the doc path related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the doc path related to the log entry.
     */
    String getDocPath();

    /**
     * Returns the doc type related to the log entry.
     * <p>
     * It might be null if the event that originated the event is not bound to
     * any document.
     *
     * @return the doc type related to the log entry.
     */
    String getDocType();

    /**
     * Return the life cycle if the document related to the log entry.
     * <p>
     * It might be null if the event that originated the event is noe bound to
     * any document.
     *
     * @return the life cycle if the document related to the log entry.
     */
    String getDocLifeCycle();

}
