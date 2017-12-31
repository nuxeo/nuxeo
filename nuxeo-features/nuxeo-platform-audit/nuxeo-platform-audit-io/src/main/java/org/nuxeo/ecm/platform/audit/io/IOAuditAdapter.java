/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.io.api.AbstractIOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.IOResources;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter for import/export of audit logs.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class IOAuditAdapter extends AbstractIOResourceAdapter {

    private static final Log log = LogFactory.getLog(IOAuditAdapter.class);

    private static final long serialVersionUID = -3661302796286246086L;

    /**
     * Should be overridden if IOLogEntryBase is subclassed.
     *
     * @return IOLogEntryBase instance that will know how to write and read log entries
     */
    protected IOLogEntryBase getLogEntryHelper() {
        return new IOLogEntryBase();
    }

    @Override
    public void setProperties(Map<String, Serializable> properties) {
    }

    /**
     * Extract logs involving given documents.
     * <p>
     * The adapter properties will filter which logs must be taken into account.
     */
    @Override
    public IOResources extractResources(String repo, Collection<DocumentRef> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(repo)) {
            Map<DocumentRef, List<LogEntry>> docLogs = new HashMap<DocumentRef, List<LogEntry>>();

            Logs logService = Framework.getService(Logs.class);

            for (DocumentRef docRef : sources) {
                try {
                    final String uuid;
                    if (docRef.type() == DocumentRef.ID) {
                        uuid = docRef.toString();
                    } else {
                        DocumentModel doc = session.getDocument(docRef);
                        uuid = doc.getId();
                    }

                    List<LogEntry> logEntries = logService.getLogEntriesFor(uuid, repo);

                    docLogs.put(docRef, logEntries);
                } catch (DocumentNotFoundException e) {
                    List<LogEntry> emptyList = Collections.emptyList();
                    docLogs.put(docRef, emptyList);
                    continue;
                }
            }
            return new IOAuditResources(docLogs);
        }
    }

    @Override
    public void getResourcesAsXML(OutputStream out, IOResources resources) {
        if (!(resources instanceof IOAuditResources)) {
            return;
        }
        IOAuditResources auditResources = (IOAuditResources) resources;

        List<LogEntry> logEntries = new ArrayList<LogEntry>();

        Map<DocumentRef, List<LogEntry>> docLogs = auditResources.getLogsMap();

        Collection<List<LogEntry>> all = docLogs.values();
        for (List<LogEntry> list : all) {
            logEntries.addAll(list);
        }

        try {
            IOLogEntryBase.write(logEntries, out);
        } catch (IOException e) {
            throw new NuxeoException("Cannot write logs", e);
        }
    }

    @Override
    public IOResources loadResourcesFromXML(InputStream stream) {
        List<LogEntry> allEntries;
        try {
            allEntries = IOLogEntryBase.read(stream);
        } catch (IOException e) {
            throw new NuxeoException("Cannot read entries from " + stream);
        }

        // will put each log entry to its correspondent document ref
        Map<DocumentRef, List<LogEntry>> docLogs = new HashMap<DocumentRef, List<LogEntry>>();
        for (LogEntry logEntry : allEntries) {
            DocumentRef docRef = new IdRef(logEntry.getDocUUID());

            List<LogEntry> logEntries = docLogs.get(docRef);
            if (logEntries == null) {
                logEntries = new ArrayList<LogEntry>();
                docLogs.put(docRef, logEntries);
            }
            logEntries.add(logEntry);
        }

        return new IOAuditResources(docLogs);
    }

    @Override
    public void storeResources(IOResources newResources) {
        if (!(newResources instanceof IOAuditResources)) {
            return;
        }
        Logs logService = Framework.getService(Logs.class);
        IOAuditResources auditResources = (IOAuditResources) newResources;
        Map<DocumentRef, List<LogEntry>> docLogs = auditResources.getLogsMap();
        for (Map.Entry<DocumentRef, List<LogEntry>> mapEntry : docLogs.entrySet()) {
            DocumentRef docRef = mapEntry.getKey();
            List<LogEntry> logs = mapEntry.getValue();
            // need to set the given docRef - so transfer with the help of
            // IOLogEntryBase (subclass eventually)
            List<LogEntry> newLogs = IOLogEntryBase.translate(logs, docRef);
            logService.addLogEntries(newLogs);
        }
    }

    @Override
    public IOResources translateResources(String repo, IOResources resources, DocumentTranslationMap map) {
        if (map == null) {
            return null;
        }
        if (!(resources instanceof IOAuditResources)) {
            return resources;
        }

        IOAuditResources auditResources = (IOAuditResources) resources;
        Map<DocumentRef, List<LogEntry>> newResourcesMap = new HashMap<DocumentRef, List<LogEntry>>();

        for (Map.Entry<DocumentRef, List<LogEntry>> entry : auditResources.getLogsMap().entrySet()) {
            DocumentRef oldRef = entry.getKey();
            DocumentRef newRef = map.getDocRefMap().get(oldRef);
            if (newRef == null) {
                if (log.isErrorEnabled()) {
                    log.error("newRef does not exist in translation map for " + oldRef);
                }
                continue;
            }
            List<LogEntry> docLogs = auditResources.getDocumentLogs(oldRef);

            // need to set the given docRef - so transfer with the help of
            // IOLogEntryBase (subclass eventually)
            List<LogEntry> newLogs = IOLogEntryBase.translate(docLogs, newRef);
            newResourcesMap.put(newRef, newLogs);
        }

        return new IOAuditResources(newResourcesMap);
    }

}
