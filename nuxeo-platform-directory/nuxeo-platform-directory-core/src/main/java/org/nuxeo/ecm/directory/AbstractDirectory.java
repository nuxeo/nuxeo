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

package org.nuxeo.ecm.directory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public abstract class AbstractDirectory implements Directory {

    protected final Log log = LogFactory.getLog(AbstractDirectory.class);

    public final String name;

    protected DirectoryFieldMapper fieldMapper;

    protected final Map<String, Reference> references = new HashMap<String, Reference>();

    // simple cache system for entry lookups, disabled by default
    protected final DirectoryCache cache;

    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected Set<Session> sessions = new HashSet<Session>();

    protected final Counter sessionCount;

    protected final Counter sessionMaxCount;

    protected AbstractDirectory(String name) {
        this.name = name;
        cache = new DirectoryCache(name);
        sessionCount = registry.counter(MetricRegistry.name(
                "nuxeo", "directories", name,  "sessions", "active"));

        sessionMaxCount =  registry.counter(MetricRegistry.name(
                "nuxeo", "directories", name,  "sessions", "max"));
    }
    /**
     * Invalidate my cache and the caches of linked directories by references.
     */
    public void invalidateCaches() throws DirectoryException {
        cache.invalidateAll();
        for (Reference ref : references.values()) {
            Directory targetDir = ref.getTargetDirectory();
            if (targetDir != null) {
                targetDir.invalidateDirectoryCache();
            }
        }
    }

    public DirectoryFieldMapper getFieldMapper() {
        if (fieldMapper == null) {
            fieldMapper = new DirectoryFieldMapper();
        }
        return fieldMapper;
    }

    @Override
    public Reference getReference(String referenceFieldName) {
        return references.get(referenceFieldName);
    }

    public boolean isReference(String referenceFieldName) {
        return references.containsKey(referenceFieldName);
    }

    public void addReference(Reference reference) throws ClientException {
        reference.setSourceDirectoryName(getName());
        references.put(reference.getFieldName(), reference);
    }

    public void addReferences(Reference[] references) throws ClientException {
        for (Reference reference : references) {
            addReference(reference);
        }
    }

    @Override
    public Collection<Reference> getReferences() {
        return references.values();
    }

    /**
     * Helper method to order entries.
     *
     * @param entries the list of entries.
     * @param orderBy an ordered map of field name -> "asc" or "desc".
     */
    public void orderEntries(List<DocumentModel> entries,
            Map<String, String> orderBy) throws DirectoryException {
        Collections.sort(entries, new DocumentModelComparator(getSchema(),
                orderBy));
    }

    @Override
    public DirectoryCache getCache() {
        return cache;
    }

    public synchronized void removeSession(Session session) {
        if (sessions.remove(session)) {
            sessionCount.dec();
        }
    }

    public synchronized void addSession(Session session) {
        sessions.add(session);
        sessionCount.inc();
        if (sessionCount.getCount() > sessionMaxCount.getCount()) {
            sessionMaxCount.inc();
        }
    }



    @Override
    public void invalidateDirectoryCache() throws DirectoryException{
        getCache().invalidateAll();
    }

    @Override
    public boolean isMultiTenant() {
        return false;
    }
    @Override
    public synchronized void shutdown() {
        Set<Session> lastSessions = sessions;
        sessions = new HashSet<Session>();
        sessionCount.dec(sessionCount.getCount());
        sessionMaxCount.dec(sessionMaxCount.getCount());
        for (Session session : lastSessions) {
            try {
                session.close();
            } catch (DirectoryException e) {
                log.error("Error during shutdown of directory '" + name
                        + "'", e);
            }
        }
    }

}
