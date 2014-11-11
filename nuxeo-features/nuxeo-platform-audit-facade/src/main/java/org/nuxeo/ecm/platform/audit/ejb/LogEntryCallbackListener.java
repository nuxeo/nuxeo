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
 * $Id: LogEntryCallbackListener.java 30086 2008-02-12 16:08:51Z ogrisel $
 */

package org.nuxeo.ecm.platform.audit.ejb;

import javax.persistence.PostPersist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Callback listener for LogEntryImpl.
 * <p>
 * Only used here to provide acccurate logs
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LogEntryCallbackListener {

    private static final Log log = LogFactory
            .getLog(LogEntryCallbackListener.class);

    @PostPersist
    public void doPostPersist(LogEntryImpl logEntry) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "LogEntryImpl with id '%s' after event '%s' for document '%s' (ref: '%s')",
                    Long.valueOf(logEntry.getId()), logEntry.getEventId(),
                    logEntry.getDocPath(), logEntry.getDocUUID()));
        }
    }

}
