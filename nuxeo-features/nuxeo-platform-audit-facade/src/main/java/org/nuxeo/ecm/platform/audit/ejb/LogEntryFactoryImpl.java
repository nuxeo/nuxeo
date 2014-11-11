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
 * $Id: LogEntryFactoryImpl.java 21918 2007-07-04 10:43:02Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.ejb;

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntryBase;
import org.nuxeo.ecm.platform.audit.api.LogEntryFactory;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;

/**
 * Default log entry factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class LogEntryFactoryImpl implements LogEntryFactory {

    private static final long serialVersionUID = 6209996442783241995L;

    private static final Log log = LogFactory.getLog(LogEntryFactoryImpl.class);

    public final LogEntry computeLogEntryFrom(DocumentMessage doc)
            throws Exception {
        @SuppressWarnings("unchecked")
        Class<LogEntry> klass = getLogEntryClass();
        LogEntry logEntry = null;
        if (klass != null) {
            @SuppressWarnings("unchecked")
            Class[] paramClasses = new Class[1];
            paramClasses[0] = DocumentMessage.class;
            Constructor<LogEntry> constructor = klass
                    .getConstructor(paramClasses);
            if (constructor != null) {
                logEntry = constructor.newInstance(doc);
            }
        } else {
            log.error("Factory doesn't have any associated log entry klass !!");
        }
        return logEntry;
    }

    @SuppressWarnings("unchecked")
    public Class getLogEntryClass() {
        return LogEntryImpl.class;
    }

    public LogEntryBase createLogEntryBase(LogEntry entry) {
        LogEntryBase converted = new LogEntryBase();
        converted.setId(entry.getId());
        converted.setCategory(entry.getCategory());
        converted.setComment(entry.getComment());
        converted.setDocLifeCycle(entry.getDocLifeCycle());
        converted.setDocPath(entry.getDocPath());
        converted.setDocUUID(entry.getDocUUID());
        converted.setDocType(entry.getDocType());
        converted.setEventDate(entry.getEventDate());
        converted.setEventId(entry.getEventId());
        converted.setEventDate(entry.getEventDate());
        converted.setPrincipalName(entry.getPrincipalName());
        return converted;
    }

}
