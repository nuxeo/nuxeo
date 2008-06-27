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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.platform.audit.NXAudit;
import org.nuxeo.ecm.platform.audit.api.AuditException;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.NXAuditEvents;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
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

    Map<String, Serializable> properties;

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
        if (properties != null) {
            for (Map.Entry<String, Serializable> prop : properties.entrySet()) {
                String propName = prop.getKey();
                Serializable propValue = prop.getValue();
                // TODO
            }
        } else {
            log.warn("no props");
        }
    }

    protected static CoreSession getCoreSession(String repo) throws Exception {
        CoreSession coreSession;
        try {
            Framework.login();
            RepositoryManager manager = Framework.getService(RepositoryManager.class);
            coreSession = manager.getRepository(repo).open();
        } catch (Exception e) {
            throw new ClientException(
                    "Failed to open core session to repository " + repo, e);
        }
        return coreSession;
    }

    protected static Logs getLogService() throws Exception {
        Logs logService;
        try {
            logService = Framework.getService(Logs.class);
        } catch (Exception e) {
            throw new AuditException(e);
        }
        return logService;
    }

    public static NXAuditEvents getNXAuditEventsService() {
        return (NXAuditEvents) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
    }

    private static long createLogEntry(DocumentMessage doc) throws AuditException {
        NXAuditEvents service = NXAudit.getNXAuditEventsService();
        LogEntry logEntry = service.computeLogEntry(doc);
        Logs logService;
        try {
            logService = Framework.getService(Logs.class);
        } catch (Exception e) {
            throw new AuditException(e);
        }
        logService.addLogEntries(Arrays.asList(logEntry));
        return logEntry.getId();
    }

    /**
     * Extract logs involving given documents.
     * <p>
     * The adapter properties will filter which logs must be taken into account.
     */
    @Override
    public IOResources extractResources(String repo,
            Collection<DocumentRef> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            Map<DocumentRef, List<LogEntry>> docLogs = new HashMap<DocumentRef, List<LogEntry>>();

            CoreSession session = getCoreSession(repo);
            Logs logService = getLogService();

            for (DocumentRef docRef : sources) {
                try {
                    final String uuid;
                    if (docRef.type() == DocumentRef.ID) {
                        uuid = docRef.toString();
                    } else {
                        DocumentModel doc = session.getDocument(docRef);
                        uuid = doc.getId();
                    }

                    List<LogEntry> logEntries = logService.getLogEntriesFor(uuid);

                    docLogs.put(docRef, logEntries);
                } catch (ClientException e) {
                    List<LogEntry> emptyList = Collections.emptyList();
                    docLogs.put(docRef, emptyList);
                    continue;
                }
            }
            return new IOAuditResources(docLogs);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void getResourcesAsXML(OutputStream out, IOResources resources) {
        if (!(resources instanceof IOAuditResources)) {
            return;
        }
        IOAuditResources auditResources = (IOAuditResources) resources;
        IOLogEntryBase helper = getLogEntryHelper();

        List<LogEntry> logEntries = new ArrayList<LogEntry>();

        Map<DocumentRef, List<LogEntry>> docLogs = auditResources.getLogsMap();

        Collection<List<LogEntry>> all = docLogs.values();
        for (List<LogEntry> list : all) {
            logEntries.addAll(list);
        }

        try {
            helper.write(logEntries, out);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public IOResources loadResourcesFromXML(InputStream stream) {

        IOLogEntryBase logEntriesHelper = getLogEntryHelper();
        List<LogEntry> allEntries;
        try {
            allEntries = logEntriesHelper.read(stream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            // TODO need to define IOResourcesException
            e.printStackTrace();
            return null;
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

        IOAuditResources auditResources = (IOAuditResources) newResources;

        Map<DocumentRef, List<LogEntry>> docLogs = auditResources.getLogsMap();

        try {
            for (Map.Entry<DocumentRef, List<LogEntry>> mapEntry : docLogs.entrySet()) {

                DocumentRef docRef = mapEntry.getKey();

                List<LogEntry> logs = mapEntry.getValue();

                // need to set the given docRef - so transfer with the help of
                // IOLogEntryBase (subclass eventually)
                IOLogEntryBase helper = getLogEntryHelper();
                List<LogEntry> newLogs = helper.translate(logs, docRef);

                getLogService().addLogEntries(newLogs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IOResources translateResources(String repo, IOResources resources,
            DocumentTranslationMap map) {
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
                log.error("newRef does not exist in translation map for " + oldRef);
                continue;
            }
            List<LogEntry> docLogs = auditResources.getDocumentLogs(oldRef);

            // need to set the given docRef - so transfer with the help of
            // IOLogEntryBase (subclass eventually)
            IOLogEntryBase helper = getLogEntryHelper();
            List<LogEntry> newLogs = helper.translate(docLogs, newRef);

            /*
             * try { getLogService().addLogEntries(newLogs); } catch
             * (AuditException e) { // TODO Auto-generated catch block
             * e.printStackTrace(); } catch (Exception e) { // TODO
             * Auto-generated catch block e.printStackTrace(); }
             */

            newResourcesMap.put(newRef, newLogs);
        }

        return new IOAuditResources(newResourcesMap);
    }
}
