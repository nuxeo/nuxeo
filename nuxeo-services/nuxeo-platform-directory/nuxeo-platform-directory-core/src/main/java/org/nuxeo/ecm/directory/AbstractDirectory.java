/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

public abstract class AbstractDirectory implements Directory {

    public static final String TENANT_ID_FIELD = "tenantId";

    public final BaseDirectoryDescriptor descriptor;

    protected DirectoryFieldMapper fieldMapper;

    protected final Map<String, List<Reference>> references = new HashMap<>();

    // simple cache system for entry lookups, disabled by default
    protected final DirectoryCache cache;

    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter sessionCount;

    protected final Counter sessionMaxCount;

    protected Map<String, Field> schemaFieldMap;

    protected List<String> types = new ArrayList<>();

    protected Class<? extends Reference> referenceClass;

    protected AbstractDirectory(BaseDirectoryDescriptor descriptor, Class<? extends Reference> referenceClass) {
        this.referenceClass = referenceClass;
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

        sessionCount = registry.counter(MetricRegistry.name("nuxeo", "directories", getName(), "sessions", "active"));
        sessionMaxCount = registry.counter(MetricRegistry.name("nuxeo", "directories", getName(), "sessions", "max"));

        // add references
        addInverseReferences(descriptor.getInverseReferences());
        addReferences(descriptor.getReferences());

        // cache parameterization
        cache = new DirectoryCache(getName());
        cache.setEntryCacheName(descriptor.cacheEntryName);
        cache.setEntryCacheWithoutReferencesName(descriptor.cacheEntryWithoutReferencesName);
        cache.setNegativeCaching(descriptor.negativeCaching);

    }

    protected boolean doSanityChecks() {
        return true;
    }

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

    @Override
    public void invalidateCaches() {
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
            throw new DirectoryException(
                    "Unexpected multiple references for " + referenceFieldName + " in directory " + getName());
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

    protected void addInverseReferences(InverseReferenceDescriptor[] references) {
        if (references != null) {
            Arrays.stream(references).map(InverseReference::new).forEach(this::addReference);
        }
    }

    protected void addReferences(ReferenceDescriptor[] references) {
        if (references != null) {
            for (ReferenceDescriptor desc : references) {
                try {
                    addReference(referenceClass.getDeclaredConstructor(ReferenceDescriptor.class).newInstance(desc));
                } catch (ReflectiveOperationException e) {
                    throw new DirectoryException(
                            "An error occurred while instantiating reference class " + referenceClass.getName(), e);
                }
            }
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
    public void orderEntries(List<DocumentModel> entries, Map<String, String> orderBy) {
        entries.sort(new DocumentModelComparator(getSchema(), orderBy));
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
    public void invalidateDirectoryCache() {
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

    /*
     * Initializes schemaFieldMap. Note that this cannot be called from the Directory constructor because the
     * SchemaManager initialization itself requires access to directories (and therefore their construction) for fields
     * having entry resolvers. So an infinite recursion must be avoided.
     */
    protected void initSchemaFieldMap() {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Schema schema = schemaManager.getSchema(getSchema());
        if (schema == null) {
            throw new DirectoryException(
                    "Invalid configuration for directory: " + getName() + ", no such schema: " + getSchema());
        }
        schemaFieldMap = new LinkedHashMap<>();
        schema.getFields().forEach(f -> schemaFieldMap.put(f.getName().getLocalName(), f));
    }

    @Override
    public Map<String, Field> getSchemaFieldMap() {
        return schemaFieldMap;
    }

    protected void fallbackOnDefaultCache() {
        CacheService cacheService = Framework.getService(CacheService.class);
        if (cacheService != null) {
            if (descriptor.cacheEntryName == null) {
                String cacheEntryName = "cache-" + getName();
                cache.setEntryCacheName(cacheEntryName);
                cacheService.registerCache(cacheEntryName);
            }
            if (descriptor.cacheEntryWithoutReferencesName == null) {
                String cacheEntryWithoutReferencesName = "cacheWithoutReference-" + getName();
                cache.setEntryCacheWithoutReferencesName(cacheEntryWithoutReferencesName);
                cacheService.registerCache(cacheEntryWithoutReferencesName);
            }
        }
    }
}
