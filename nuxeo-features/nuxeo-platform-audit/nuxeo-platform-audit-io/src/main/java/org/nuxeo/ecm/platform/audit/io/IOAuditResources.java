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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.io;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.io.api.IOResources;

/**
 * IO Resources for logs
 * <p>
 * Holds a map of document resources, with a document reference as key, and a
 * list of RDF resources as values.
 */
public class IOAuditResources implements IOResources {

    private static final long serialVersionUID = 8095413628515011558L;

    private Map<DocumentRef, List<LogEntry>> docLogs = new HashMap<DocumentRef, List<LogEntry>>();

    public IOAuditResources(Map<DocumentRef, List<LogEntry>> docLogs) {
        this.docLogs = docLogs;
    }

    public List<LogEntry> getDocumentLogs(DocumentRef docRef) {
        List<LogEntry> logs = docLogs.get(docRef);
        if (logs != null) {
            return Collections.unmodifiableList(logs);
        }
        return null;
    }

    public Map<DocumentRef, List<LogEntry>> getLogsMap() {
        return Collections.unmodifiableMap(docLogs);
    }

}
