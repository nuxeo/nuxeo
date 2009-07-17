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
 *     anguenot
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.search.resources.indexing;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.AbstractIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.security.IndexingSecurityConstants;
import org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.Logs;
import org.nuxeo.ecm.platform.audit.api.AuditRuntimeException;
import org.nuxeo.ecm.platform.audit.search.resources.indexing.api.AuditIndexableResource;
import org.nuxeo.runtime.api.Framework;

/**
 * Audit indexable resource implementation.
 * <p>
 * Takes care of fetching log entries, generating log entry data maps and
 * returns log entry data given a key.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class AuditIndexableResourceImpl extends AbstractIndexableResource
        implements AuditIndexableResource {

    private static final long serialVersionUID = 1L;

    protected final long logUUID;

    protected Logs auditLogsService;

    public AuditIndexableResourceImpl() {
        this(0L);
    }

    public AuditIndexableResourceImpl(long logUUID) {
        this.logUUID = logUUID;
    }

    public Serializable getValueFor(String indexableDataName)
            throws IndexingException {
        login();
        Serializable value = getLogEntryDataMapFor(logUUID).get(
                indexableDataName);
        logout();
        return value;
    }

    public Map<String, Serializable> getLogEntryDataMapFor(long id)
            throws IndexingException {
        Logs logsRemote = getAuditLogsService();

        if (logsRemote == null) {
            throw new IndexingException("LogsBean remote stub is null...");
        }

        LogEntry entry = logsRemote.getLogEntryByID(logUUID);

        if (entry == null) {
            throw new IndexingException(
                    "Impossible to get the corresponding log entry...");
        }
        return getLogEntryDataMap(entry);
    }

    protected Logs getAuditLogsService() {
        if (auditLogsService == null) {
            try {
                auditLogsService = Framework.getService(Logs.class);
            } catch (Exception e) {
                throw new AuditRuntimeException("Cannot locate remote logs audit", e);
            }
        }
        return auditLogsService;
    }

    protected static Map<String, Serializable> getLogEntryDataMap(LogEntry entry) {
        Map<String, Serializable> dataMap = new HashMap<String, Serializable>();
        dataMap.put(BuiltinLogEntryData.LOG_ID, entry.getId());
        dataMap.put(BuiltinLogEntryData.LOG_EVENT_ID, entry.getEventId());
        dataMap.put(BuiltinLogEntryData.LOG_EVENT_DATE, entry.getEventDate());
        dataMap.put(BuiltinLogEntryData.LOG_DOC_UUID, entry.getDocUUID());
        dataMap.put(BuiltinLogEntryData.LOG_DOC_PATH, entry.getDocPath());
        dataMap.put(BuiltinLogEntryData.LOG_DOC_TYPE, entry.getDocType());
        dataMap.put(BuiltinLogEntryData.LOG_PRINCIPAL_NAME,
                entry.getPrincipalName());
        dataMap.put(BuiltinLogEntryData.LOG_COMMENT, entry.getComment());
        dataMap.put(BuiltinLogEntryData.LOG_CATEGORY, entry.getCategory());
        dataMap.put(BuiltinLogEntryData.LOG_DOC_LIFE_CYCLE,
                entry.getDocLifeCycle());
        return dataMap;
    }

    public String computeId() {
        return Long.toString(logUUID);
    }

    @Override
    // TODO for JA: validate this (no specific security filtering
    // on audit index)
    public ACP computeAcp() {
        return IndexingSecurityConstants.getOpenAcp();
    }

}
