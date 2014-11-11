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
 * $Id: LogEntryFactory.java 21918 2007-07-04 10:43:02Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.api;

import java.io.Serializable;

import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * Log entry factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface LogEntryFactory extends Serializable {

    /**
     * Computes a a log entry from a document message.
     *
     * @param doc the document message instance
     * @return a LogEntry instance.
     */
    LogEntry computeLogEntryFrom(DocumentMessage doc) throws Exception;

    /**
     * Returns the class this factory will instantiate.
     *
     * @return a LogEntry class definition.
     */
    Class<LogEntry> getLogEntryClass();

    /**
     * Converts a log entry entity to a log entry base.
     *
     * <p>
     * This method is used to generate remotable object for the API by
     * <code>LogsBean</code> internals.
     * </p>
     *
     * @param entry : a log entry entity
     * @return a a log entry base instance.
     */
    LogEntryBase createLogEntryBase(LogEntry entry);

}
