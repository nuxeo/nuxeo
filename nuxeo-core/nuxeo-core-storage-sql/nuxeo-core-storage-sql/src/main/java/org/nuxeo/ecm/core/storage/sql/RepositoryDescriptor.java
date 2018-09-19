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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.FulltextDescriptor.FulltextIndexDescriptor;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;

/**
 * Low-level VCS Repository Descriptor.
 */
@XObject(value = "repository", order = { "@name" })
public class RepositoryDescriptor {

    private static final Log log = LogFactory.getLog(RepositoryDescriptor.class);

    public static final int DEFAULT_READ_ACL_MAX_SIZE = 4096;

    public static final int DEFAULT_PATH_OPTIM_VERSION = 2;

    /** At startup, DDL changes are not detected. */
    public static final String DDL_MODE_IGNORE = "ignore";

    /** At startup, DDL changes are detected and if not empty they are dumped. */
    public static final String DDL_MODE_DUMP = "dump";

    /** At startup, DDL changes are detected and executed. */
    public static final String DDL_MODE_EXECUTE = "execute";

    /** At startup, DDL changes are detected and if not empty Nuxeo startup is aborted. */
    public static final String DDL_MODE_ABORT = "abort";

    /** Specifies that stored procedure detection must be compatible with previous Nuxeo versions. */
    public static final String DDL_MODE_COMPAT = "compat";

    @XObject(value = "field")
    public static class FieldDescriptor {

        // empty constructor needed by XMap
        public FieldDescriptor() {
        }

        /** Copy constructor. */
        public FieldDescriptor(FieldDescriptor other) {
            type = other.type;
            field = other.field;
            table = other.table;
            column = other.column;
        }

        public static List<FieldDescriptor> copyList(List<FieldDescriptor> other) {
            List<FieldDescriptor> copy = new ArrayList<>(other.size());
            for (FieldDescriptor fd : other) {
                copy.add(new FieldDescriptor(fd));
            }
            return copy;
        }

        public void merge(FieldDescriptor other) {
            if (other.field != null) {
                field = other.field;
            }
            if (other.type != null) {
                type = other.type;
            }
            if (other.table != null) {
                table = other.table;
            }
            if (other.column != null) {
                column = other.column;
            }
        }

        @XNode("@type")
        public String type;

        public String field;

        @XNode("@name")
        public void setName(String name) {
            if (!StringUtils.isBlank(name) && field == null) {
                field = name;
            }
        }

        // compat with older syntax
        @XNode
        public void setXNodeContent(String name) {
            setName(name);
        }

        @XNode("@table")
        public String table;

        @XNode("@column")
        public String column;

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + '(' + field + ",type=" + type + ",table=" + table + ",column="
                    + column + ")";
        }
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    /** True if the boolean is null or TRUE, false otherwise. */
    private static boolean defaultTrue(Boolean bool) {
        return !Boolean.FALSE.equals(bool);
    }

    public String name;

    @XNode("@name")
    public void setName(String name) {
        this.name = name;
    }

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
    }

    // compat, when used with old-style extension point syntax
    // and nested repository
    @XNode("repository")
    public RepositoryDescriptor repositoryDescriptor;

    public NuxeoConnectionManagerConfiguration pool;

    @XNode("pool")
    public void setPool(NuxeoConnectionManagerConfiguration pool) {
        pool.setName("repository/" + name);
        this.pool = pool;
    }

    @XNode("backendClass")
    public Class<? extends RepositoryBackend> backendClass;

    @XNode("clusterInvalidatorClass")
    public Class<? extends ClusterInvalidator> clusterInvalidatorClass;

    @XNode("cachingMapper@class")
    public Class<? extends CachingMapper> cachingMapperClass;

    @XNode("cachingMapper@enabled")
    private Boolean cachingMapperEnabled;

    public boolean getCachingMapperEnabled() {
        return defaultTrue(cachingMapperEnabled);
    }

    @XNodeMap(value = "cachingMapper/property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> cachingMapperProperties = new HashMap<>();

    @XNode("ddlMode")
    private String ddlMode;

    public String getDDLMode() {
        return ddlMode;
    }

    @XNode("noDDL")
    private Boolean noDDL;

    public boolean getNoDDL() {
        return defaultFalse(noDDL);
    }

    @XNodeList(value = "sqlInitFile", type = ArrayList.class, componentType = String.class)
    public List<String> sqlInitFiles = new ArrayList<>(0);

    @XNode("softDelete@enabled")
    private Boolean softDeleteEnabled;

    public boolean getSoftDeleteEnabled() {
        return defaultFalse(softDeleteEnabled);
    }

    protected void setSoftDeleteEnabled(boolean enabled) {
        softDeleteEnabled = Boolean.valueOf(enabled);
    }

    @XNode("proxies@enabled")
    private Boolean proxiesEnabled;

    public boolean getProxiesEnabled() {
        return defaultTrue(proxiesEnabled);
    }

    protected void setProxiesEnabled(boolean enabled) {
        proxiesEnabled = Boolean.valueOf(enabled);
    }

    @XNode("idType")
    public String idType; // "varchar", "uuid", "sequence"

    @XNode("clustering@id")
    private String clusterNodeId;

    public String getClusterNodeId() {
        return clusterNodeId;
    }

    @XNode("clustering@enabled")
    private Boolean clusteringEnabled;

    public boolean getClusteringEnabled() {
        return defaultFalse(clusteringEnabled);
    }

    protected void setClusteringEnabled(boolean enabled) {
        clusteringEnabled = Boolean.valueOf(enabled);
    }

    @XNode("clustering@delay")
    private Long clusteringDelay;

    public long getClusteringDelay() {
        return clusteringDelay == null ? 0 : clusteringDelay.longValue();
    }

    protected void setClusteringDelay(long delay) {
        clusteringDelay = Long.valueOf(delay);
    }

    @XNodeList(value = "schema/field", type = ArrayList.class, componentType = FieldDescriptor.class)
    public List<FieldDescriptor> schemaFields = new ArrayList<>(0);

    @XNode("schema/arrayColumns")
    private Boolean arrayColumns;

    public boolean getArrayColumns() {
        return defaultFalse(arrayColumns);
    }

    public void setArrayColumns(boolean enabled) {
        arrayColumns = Boolean.valueOf(enabled);
    }

    @XNode("childNameUniqueConstraintEnabled")
    private Boolean childNameUniqueConstraintEnabled;

    public boolean getChildNameUniqueConstraintEnabled() {
        return defaultTrue(childNameUniqueConstraintEnabled);
    }

    @XNode("collectionUniqueConstraintEnabled")
    private Boolean collectionUniqueConstraintEnabled;

    public boolean getCollectionUniqueConstraintEnabled() {
        return defaultTrue(collectionUniqueConstraintEnabled);
    }

    @XNode("indexing/queryMaker@class")
    public void setQueryMakerDeprecated(String klass) {
        log.warn("Setting queryMaker from repository configuration is now deprecated");
    }

    // VCS-specific fulltext indexing options
    private String fulltextAnalyzer;

    public String getFulltextAnalyzer() {
        return fulltextAnalyzer;
    }

    @XNode("indexing/fulltext@analyzer")
    public void setFulltextAnalyzer(String fulltextAnalyzer) {
        this.fulltextAnalyzer = fulltextAnalyzer;
    }

    private String fulltextCatalog;

    public String getFulltextCatalog() {
        return fulltextCatalog;
    }

    @XNode("indexing/fulltext@catalog")
    public void setFulltextCatalog(String fulltextCatalog) {
        this.fulltextCatalog = fulltextCatalog;
    }

    private FulltextDescriptor fulltextDescriptor = new FulltextDescriptor();

    public FulltextDescriptor getFulltextDescriptor() {
        return fulltextDescriptor;
    }

    @XNode("indexing/fulltext@fieldSizeLimit")
    public void setFulltextFieldSizeLimit(int fieldSizeLimit) {
        fulltextDescriptor.setFulltextFieldSizeLimit(fieldSizeLimit);
    }

    @XNode("indexing/fulltext@disabled")
    public void setFulltextDisabled(boolean disabled) {
        fulltextDescriptor.setFulltextDisabled(disabled);
    }

    @XNode("indexing/fulltext@searchDisabled")
    public void setFulltextSearchDisabled(boolean disabled) {
        fulltextDescriptor.setFulltextSearchDisabled(disabled);
    }

    @XNodeList(value = "indexing/fulltext/index", type = ArrayList.class, componentType = FulltextIndexDescriptor.class)
    public void setFulltextIndexes(List<FulltextIndexDescriptor> fulltextIndexes) {
        fulltextDescriptor.setFulltextIndexes(fulltextIndexes);
    }

    @XNodeList(value = "indexing/excludedTypes/type", type = HashSet.class, componentType = String.class)
    public void setFulltextExcludedTypes(Set<String> fulltextExcludedTypes) {
        fulltextDescriptor.setFulltextExcludedTypes(fulltextExcludedTypes);
    }

    @XNodeList(value = "indexing/includedTypes/type", type = HashSet.class, componentType = String.class)
    public void setFulltextIncludedTypes(Set<String> fulltextIncludedTypes) {
        fulltextDescriptor.setFulltextIncludedTypes(fulltextIncludedTypes);
    }

    // compat
    @XNodeList(value = "indexing/neverPerDocumentFacets/facet", type = HashSet.class, componentType = String.class)
    public Set<String> neverPerInstanceMixins = new HashSet<>(0);

    @XNode("pathOptimizations@enabled")
    private Boolean pathOptimizationsEnabled;

    public boolean getPathOptimizationsEnabled() {
        return defaultTrue(pathOptimizationsEnabled);
    }

    protected void setPathOptimizationsEnabled(boolean enabled) {
        pathOptimizationsEnabled = Boolean.valueOf(enabled);
    }

    /* @since 5.7 */
    @XNode("pathOptimizations@version")
    private Integer pathOptimizationsVersion;

    public int getPathOptimizationsVersion() {
        return pathOptimizationsVersion == null ? DEFAULT_PATH_OPTIM_VERSION : pathOptimizationsVersion.intValue();
    }

    @XNode("aclOptimizations@enabled")
    private Boolean aclOptimizationsEnabled;

    public boolean getAclOptimizationsEnabled() {
        return defaultTrue(aclOptimizationsEnabled);
    }

    protected void setAclOptimizationsEnabled(boolean enabled) {
        aclOptimizationsEnabled = Boolean.valueOf(enabled);
    }

    /* @since 5.4.2 */
    @XNode("aclOptimizations@readAclMaxSize")
    private Integer readAclMaxSize;

    public int getReadAclMaxSize() {
        return readAclMaxSize == null ? DEFAULT_READ_ACL_MAX_SIZE : readAclMaxSize.intValue();
    }

    @XNode("usersSeparator@key")
    public String usersSeparatorKey;

    /** @since 9.1 */
    @XNode("changeTokenEnabled")
    private Boolean changeTokenEnabled;

    /** @since 9.1 */
    public boolean isChangeTokenEnabled() {
        return defaultFalse(changeTokenEnabled);
    }

    /** @since 9.1 */
    public void setChangeTokenEnabled(boolean enabled) {
        this.changeTokenEnabled = Boolean.valueOf(enabled);
    }

    public RepositoryDescriptor() {
    }

    /** Copy constructor. */
    public RepositoryDescriptor(RepositoryDescriptor other) {
        name = other.name;
        label = other.label;
        isDefault = other.isDefault;
        pool = other.pool == null ? null : new NuxeoConnectionManagerConfiguration(other.pool);
        backendClass = other.backendClass;
        clusterInvalidatorClass = other.clusterInvalidatorClass;
        cachingMapperClass = other.cachingMapperClass;
        cachingMapperEnabled = other.cachingMapperEnabled;
        cachingMapperProperties = new HashMap<>(other.cachingMapperProperties);
        noDDL = other.noDDL;
        ddlMode = other.ddlMode;
        sqlInitFiles = new ArrayList<>(other.sqlInitFiles);
        softDeleteEnabled = other.softDeleteEnabled;
        proxiesEnabled = other.proxiesEnabled;
        schemaFields = FieldDescriptor.copyList(other.schemaFields);
        arrayColumns = other.arrayColumns;
        childNameUniqueConstraintEnabled = other.childNameUniqueConstraintEnabled;
        collectionUniqueConstraintEnabled = other.collectionUniqueConstraintEnabled;
        idType = other.idType;
        clusterNodeId = other.clusterNodeId;
        clusteringEnabled = other.clusteringEnabled;
        clusteringDelay = other.clusteringDelay;
        fulltextAnalyzer = other.fulltextAnalyzer;
        fulltextCatalog = other.fulltextCatalog;
        fulltextDescriptor = new FulltextDescriptor(other.fulltextDescriptor);
        neverPerInstanceMixins = other.neverPerInstanceMixins;
        pathOptimizationsEnabled = other.pathOptimizationsEnabled;
        pathOptimizationsVersion = other.pathOptimizationsVersion;
        aclOptimizationsEnabled = other.aclOptimizationsEnabled;
        readAclMaxSize = other.readAclMaxSize;
        usersSeparatorKey = other.usersSeparatorKey;
        changeTokenEnabled = other.changeTokenEnabled;
    }

    public void merge(RepositoryDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.label != null) {
            label = other.label;
        }
        if (other.isDefault != null) {
            isDefault = other.isDefault;
        }
        if (other.pool != null) {
            if (pool == null) {
                pool = new NuxeoConnectionManagerConfiguration(other.pool);
            } else {
                pool.merge(other.pool);
            }
        }
        if (other.backendClass != null) {
            backendClass = other.backendClass;
        }
        if (other.clusterInvalidatorClass != null) {
            clusterInvalidatorClass = other.clusterInvalidatorClass;
        }
        if (other.cachingMapperClass != null) {
            cachingMapperClass = other.cachingMapperClass;
        }
        if (other.cachingMapperEnabled != null) {
            cachingMapperEnabled = other.cachingMapperEnabled;
        }
        cachingMapperProperties.putAll(other.cachingMapperProperties);
        if (other.noDDL != null) {
            noDDL = other.noDDL;
        }
        if (other.ddlMode != null) {
            ddlMode = other.ddlMode;
        }
        sqlInitFiles.addAll(other.sqlInitFiles);
        if (other.softDeleteEnabled != null) {
            softDeleteEnabled = other.softDeleteEnabled;
        }
        if (other.proxiesEnabled != null) {
            proxiesEnabled = other.proxiesEnabled;
        }
        if (other.idType != null) {
            idType = other.idType;
        }
        if (other.clusterNodeId != null) {
            clusterNodeId = other.clusterNodeId;
        }
        if (other.clusteringEnabled != null) {
            clusteringEnabled = other.clusteringEnabled;
        }
        if (other.clusteringDelay != null) {
            clusteringDelay = other.clusteringDelay;
        }
        for (FieldDescriptor of : other.schemaFields) {
            boolean append = true;
            for (FieldDescriptor f : schemaFields) {
                if (f.field.equals(of.field)) {
                    f.merge(of);
                    append = false;
                    break;
                }
            }
            if (append) {
                schemaFields.add(of);
            }
        }
        if (other.arrayColumns != null) {
            arrayColumns = other.arrayColumns;
        }
        if (other.childNameUniqueConstraintEnabled != null) {
            childNameUniqueConstraintEnabled = other.childNameUniqueConstraintEnabled;
        }
        if (other.collectionUniqueConstraintEnabled != null) {
            collectionUniqueConstraintEnabled = other.collectionUniqueConstraintEnabled;
        }
        if (other.fulltextAnalyzer != null) {
            fulltextAnalyzer = other.fulltextAnalyzer;
        }
        if (other.fulltextCatalog != null) {
            fulltextCatalog = other.fulltextCatalog;
        }
        fulltextDescriptor.merge(other.fulltextDescriptor);
        neverPerInstanceMixins.addAll(other.neverPerInstanceMixins);
        if (other.pathOptimizationsEnabled != null) {
            pathOptimizationsEnabled = other.pathOptimizationsEnabled;
        }
        if (other.pathOptimizationsVersion != null) {
            pathOptimizationsVersion = other.pathOptimizationsVersion;
        }
        if (other.aclOptimizationsEnabled != null) {
            aclOptimizationsEnabled = other.aclOptimizationsEnabled;
        }
        if (other.readAclMaxSize != null) {
            readAclMaxSize = other.readAclMaxSize;
        }
        if (other.usersSeparatorKey != null) {
            usersSeparatorKey = other.usersSeparatorKey;
        }
        if (other.changeTokenEnabled != null) {
            changeTokenEnabled = other.changeTokenEnabled;
        }
    }

}
