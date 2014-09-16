/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Tiry
 * 
 */
package org.nuxeo.elasticsearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.nuxeo.ecm.platform.audit.api.FilterMapEntry;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AbstractAuditBackend;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.BaseLogEntryProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation of the {@link AuditBackend} interface using Elasticsearch persistence
 * 
 * @author tiry
 *
 */
public class ESAuditBackend extends AbstractAuditBackend implements
        AuditBackend {

    protected Client esClient = null;

    protected BaseLogEntryProvider provider = null;

    protected Object getClient() {
        if (esClient == null) {
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esClient = esa.getClient();
        }
        return esClient;
    }

    @Override
    public void deactivate() throws Exception {
        if (esClient != null) {
            esClient.close();
        }
    }

   
    @Override
    public List<LogEntry> getLogEntriesFor(String uuid,
            Map<String, FilterMapEntry> filterMap, boolean doDefaultSort) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public LogEntry getLogEntryByID(long id) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<?> nativeQuery(String query, Map<String, Object> params,
            int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, String dateRange,
            String[] categories, String path, int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public List<LogEntry> queryLogsByPage(String[] eventIds, Date limit,
            String[] categories, String path, int pageNb, int pageSize) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    @Override
    public void addLogEntries(List<LogEntry> entries) {
        throw new UnsupportedOperationException("Not implemented yet!");

    }

    @Override
    public Long getEventsCount(String eventId) {
        throw new UnsupportedOperationException("Not implemented yet!");
    }

    
    protected BaseLogEntryProvider getProvider() {

        if (provider == null) {
            provider = new BaseLogEntryProvider() {

                @Override
                public int removeEntries(String eventId, String pathPattern) {
                    throw new UnsupportedOperationException(
                            "Not implemented yet!");
                }

                @Override
                public void addLogEntry(LogEntry logEntry) {
                    List<LogEntry> entries = new ArrayList<>();
                    entries.add(logEntry);
                    addLogEntries(entries);
                }
            };
        }
        return provider;
    }

    @Override
    public long syncLogCreationEntries(final String repoId, final String path,
            final Boolean recurs) {
        return syncLogCreationEntries(getProvider(), repoId, path, recurs);
    }

}
