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

package org.nuxeo.ecm.platform.audit.api;

import java.util.Set;

import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * NXAuditEvents interface.
 * <p>
 * Allows to query for auditable events.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface NXAuditEvents {

    /**
     * Returns the list of auditable event names.
     *
     * @return list of String representing event names.
     */
    Set<String> getAuditableEventNames();

    /**
     * Returns the log entry factory class.
     *
     * @return a LogEntryFactory class
     */
    Class<LogEntryFactory> getLogEntryFactoryKlass();

    /**
     * Returns a log entry factory instance.
     *
     * @return a LogEntryFactory instance.
     */
    LogEntryFactory getLogEntryFactory();

    /**
     * Computes a log entry given a document message instance.
     *
     * @param doc
     *            the document message instance.
     * @return a log entry instance.
     */
    LogEntry computeLogEntry(DocumentMessage doc);

}
