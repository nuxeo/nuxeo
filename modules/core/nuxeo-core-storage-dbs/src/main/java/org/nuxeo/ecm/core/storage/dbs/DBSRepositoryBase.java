/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.CompositeType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.storage.FulltextConfigurationFactory;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.lock.LockManagerService;
import org.nuxeo.runtime.api.Framework;

/**
 * Provides sharing behavior for repository sessions and other basic functions.
 *
 * @since 5.9.4
 */
public abstract class DBSRepositoryBase implements DBSRepository {

    private static final Log log = LogFactory.getLog(DBSRepositoryBase.class);

    public static final String TYPE_ROOT = "Root";

    // change to have deterministic pseudo-UUID generation for debugging
    public static final boolean DEBUG_UUIDS = false;

    public static final String UUID_ZERO = "00000000-0000-0000-0000-000000000000";

    public static final String UUID_ZERO_DEBUG = "UUID_0";

    /**
     * Type of id to used for documents.
     *
     * @since 8.3
     */
    public enum IdType {
        /** Random UUID stored in a string. */
        varchar,
        /** Random UUID stored as a native UUID type. */
        uuid,
        /** Integer sequence maintained by the database. */
        sequence,
        /**
         * Integer sequence maintained by the database, in a pseudo-random order, as hex.
         *
         * @since 11.1
         */
        sequenceHexRandomized,
    }

    /** @since 8.3 */
    protected final IdType idType;

    protected final String repositoryName;

    protected final FulltextConfiguration fulltextConfiguration;

    protected final BlobManager blobManager;

    protected LockManager lockManager;

    protected final boolean changeTokenEnabled;

    /**
     * @since 7.4 : used to know if the LockManager was provided by this repository or externally
     */
    protected boolean selfRegisteredLockManager = false;

    public DBSRepositoryBase(String repositoryName, DBSRepositoryDescriptor descriptor) {
        this.repositoryName = repositoryName;
        String idt = descriptor.idType;
        List<IdType> allowed = getAllowedIdTypes();
        if (StringUtils.isBlank(idt)) {
            idt = allowed.get(0).name();
        }
        try {
            idType = IdType.valueOf(idt);
            if (!allowed.contains(idType)) {
                throw new IllegalArgumentException("Invalid id type: " + idt);
            }
        } catch (IllegalArgumentException e) {
            throw new NuxeoException("Unknown id type: " + idt + ", allowed: " + allowed);
        }
        FulltextDescriptor fulltextDescriptor = descriptor.getFulltextDescriptor();
        if (fulltextDescriptor.getFulltextDisabled()) {
            fulltextConfiguration = null;
        } else {
            fulltextConfiguration = FulltextConfigurationFactory.make(fulltextDescriptor);
        }
        changeTokenEnabled = descriptor.isChangeTokenEnabled();
        blobManager = Framework.getService(BlobManager.class);
        initBlobsPaths();
        initLockManager();
    }

    /** Gets the allowed id types for this DBS repository. The first one is the default. */
    public abstract List<IdType> getAllowedIdTypes();

    /** @since 11.1 */
    public IdType getIdType() {
        return idType;
    }

    @Override
    public void shutdown() {
        if (selfRegisteredLockManager) {
            LockManagerService lms = Framework.getService(LockManagerService.class);
            if (lms != null) {
                lms.unregisterLockManager(getLockManagerName());
            }
        }
    }

    @Override
    public String getName() {
        return repositoryName;
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        return fulltextConfiguration;
    }

    protected String getLockManagerName() {
        // TODO configure in repo descriptor
        return getName();
    }

    protected void initLockManager() {
        String lockManagerName = getLockManagerName();
        LockManagerService lockManagerService = Framework.getService(LockManagerService.class);
        lockManager = lockManagerService.getLockManager(lockManagerName);
        if (lockManager == null) {
            // no descriptor, use DBS repository intrinsic lock manager
            lockManager = this;
            log.info("Repository " + repositoryName + " using own lock manager");
            lockManagerService.registerLockManager(lockManagerName, lockManager);
            selfRegisteredLockManager = true;
        } else {
            selfRegisteredLockManager = false;
            log.info("Repository " + repositoryName + " using lock manager " + lockManager);
        }
    }

    @Override
    public LockManager getLockManager() {
        return lockManager;
    }

    @Override
    public Lock getLock(String id) {
        try (DBSConnection connection = getConnection()) {
            return connection.getLock(id);
        }
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        try (DBSConnection connection = getConnection()) {
            return connection.setLock(id, lock);
        }
    }

    @Override
    public Lock removeLock(String id, String owner) {
        try (DBSConnection connection = getConnection()) {
            return connection.removeLock(id, owner);
        }
    }

    protected abstract void initBlobsPaths();

    /** Finds the paths for all blobs in all document types. */
    protected static abstract class BlobFinder {

        protected final Set<String> schemaDone = new HashSet<>();

        protected final Deque<String> path = new ArrayDeque<>();

        public void visit() {
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            // document types
            for (DocumentType docType : schemaManager.getDocumentTypes()) {
                visitSchemas(docType.getSchemas());
            }
            // mixins
            for (CompositeType type : schemaManager.getFacets()) {
                visitSchemas(type.getSchemas());
            }
        }

        protected void visitSchemas(Collection<Schema> schemas) {
            for (Schema schema : schemas) {
                if (schemaDone.add(schema.getName())) {
                    visitComplexType(schema);
                }
            }
        }

        protected void visitComplexType(ComplexType complexType) {
            if (TypeConstants.isContentType(complexType)) {
                recordBlobPath();
                return;
            }
            for (Field field : complexType.getFields()) {
                visitField(field);
            }
        }

        /** Records a blob path, stored in the {@link #path} field. */
        protected abstract void recordBlobPath();

        protected void visitField(Field field) {
            Type type = field.getType();
            if (type.isSimpleType()) {
                // scalar
                // assume no bare binary exists
            } else if (type.isComplexType()) {
                // complex property
                String name = field.getName().getPrefixedName();
                path.addLast(name);
                visitComplexType((ComplexType) type);
                path.removeLast();
            } else {
                // array or list
                Type fieldType = ((ListType) type).getFieldType();
                if (fieldType.isSimpleType()) {
                    // array
                    // assume no array of bare binaries exist
                } else {
                    // complex list
                    String name = field.getName().getPrefixedName();
                    path.addLast(name);
                    visitComplexType((ComplexType) fieldType);
                    path.removeLast();
                }
            }
        }
    }

    @Override
    public BlobManager getBlobManager() {
        return blobManager;
    }

    @Override
    public boolean isFulltextDisabled() {
        return fulltextConfiguration == null;
    }

    @Override
    public boolean isFulltextStoredInBlob() {
        return fulltextConfiguration != null && fulltextConfiguration.fulltextStoredInBlob;
    }

    @Override
    public boolean isFulltextSearchDisabled() {
        return isFulltextDisabled() || isFulltextStoredInBlob() || fulltextConfiguration.fulltextSearchDisabled;
    }

    @Override
    public boolean isChangeTokenEnabled() {
        return changeTokenEnabled;
    }

    @Override
    public Session getSession() {
        return new DBSSession(this);
    }

}
