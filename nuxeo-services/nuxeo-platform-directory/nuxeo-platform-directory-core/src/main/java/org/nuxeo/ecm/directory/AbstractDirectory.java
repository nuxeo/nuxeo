/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.directory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public abstract class AbstractDirectory implements Directory {

    public final BaseDirectoryDescriptor descriptor;

    protected DirectoryFieldMapper fieldMapper;

    protected final Map<String, List<Reference>> references = new HashMap<>();

    // simple cache system for entry lookups, disabled by default
    protected final DirectoryCache cache;

    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter sessionCount;

    protected final Counter sessionMaxCount;

    private List<String> types = new ArrayList<String>();

    protected AbstractDirectory(BaseDirectoryDescriptor descriptor) {
        this.descriptor = descriptor;
        // is the directory visible in the ui
        if (descriptor.types != null) {
            this.types = Arrays.asList(descriptor.types);
        }
        if (!descriptor.template && doSanityChecks()) {
            if (StringUtils.isEmpty(descriptor.idField)) {
                throw new DirectoryException("idField configuration is missing for directory: " + getName());
            }
            if (StringUtils.isEmpty(descriptor.schemaName)) {
                throw new DirectoryException("schema configuration is missing for directory " + getName());
            }
        }
        cache = new DirectoryCache(getName());
        sessionCount = registry.counter(MetricRegistry.name("nuxeo", "directories", getName(), "sessions", "active"));
        sessionMaxCount = registry.counter(MetricRegistry.name("nuxeo", "directories", getName(), "sessions", "max"));
    }

    protected boolean doSanityChecks() {
        return true;
    }

    /** To be implemented with a more specific return type. */
    public abstract BaseDirectoryDescriptor getDescriptor();

    @Override
    public String getName() {
        return descriptor.name;
    }

    @Override
    public String getSchema() {
        return descriptor.schemaName;
    }

    @Override
    public String getParentDirectory() {
        return descriptor.parentDirectory;
    }

    @Override
    public String getIdField() {
        return descriptor.idField;
    }

    @Override
    public String getPasswordField() {
        return descriptor.passwordField;
    }

    @Override
    public boolean isReadOnly() {
        return descriptor.isReadOnly();
    }

    public void setReadOnly(boolean readOnly) {
        descriptor.setReadOnly(readOnly);
    }

    /**
     * Invalidate my cache and the caches of linked directories by references.
     */
    public void invalidateCaches() throws DirectoryException {
        cache.invalidateAll();
        for (Reference ref : getReferences()) {
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

    @Deprecated
    @Override
    public Reference getReference(String referenceFieldName) {
        List<Reference> refs = getReferences(referenceFieldName);
        if (refs == null || refs.isEmpty()) {
            return null;
        } else if (refs.size() == 1) {
            return refs.get(0);
        } else {
            throw new DirectoryException("Unexpected multiple references for " + referenceFieldName + " in directory "
                    + getName());
        }
    }

    @Override
    public List<Reference> getReferences(String referenceFieldName) {
        return references.get(referenceFieldName);
    }

    public boolean isReference(String referenceFieldName) {
        return references.containsKey(referenceFieldName);
    }

    public void addReference(Reference reference) {
        reference.setSourceDirectoryName(getName());
        String fieldName = reference.getFieldName();
        List<Reference> fieldRefs;
        if (references.containsKey(fieldName)) {
            fieldRefs = references.get(fieldName);
        } else {
            references.put(fieldName, fieldRefs = new ArrayList<>(1));
        }
        fieldRefs.add(reference);
    }

    public void addReferences(Reference[] refs) {
        for (Reference reference : refs) {
            addReference(reference);
        }
    }

    @Override
    public Collection<Reference> getReferences() {
        List<Reference> allRefs = new ArrayList<>(2);
        for (List<Reference> refs : references.values()) {
            allRefs.addAll(refs);
        }
        return allRefs;
    }

    /**
     * Helper method to order entries.
     *
     * @param entries the list of entries.
     * @param orderBy an ordered map of field name -> "asc" or "desc".
     */
    public void orderEntries(List<DocumentModel> entries, Map<String, String> orderBy) throws DirectoryException {
        Collections.sort(entries, new DocumentModelComparator(getSchema(), orderBy));
    }

    @Override
    public DirectoryCache getCache() {
        return cache;
    }

    public void removeSession(Session session) {
        sessionCount.dec();
    }

    public void addSession(Session session) {
        sessionCount.inc();
        if (sessionCount.getCount() > sessionMaxCount.getCount()) {
            sessionMaxCount.inc();
        }
    }

    @Override
    public void invalidateDirectoryCache() throws DirectoryException {
        getCache().invalidateAll();
    }

    @Override
    public boolean isMultiTenant() {
        return false;
    }

    @Override
    public void shutdown() {
        sessionCount.dec(sessionCount.getCount());
        sessionMaxCount.dec(sessionMaxCount.getCount());
    }

    /**
     * since @8.4
     */
    @Override
    public List<String> getTypes() {
        return types;
    }

    /**
     * @since 8.4
     */
    @Override
    public List<DirectoryDeleteConstraint> getDirectoryDeleteConstraints() {
        return descriptor.getDeleteConstraints();
    }

}
