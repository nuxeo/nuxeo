/*
 * (C) Copyright 2016-2021 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.repository.PoolConfiguration;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.FulltextDescriptor.FulltextIndexDescriptor;

/**
 * DBS Repository Descriptor.
 *
 * @since 7.10-HF04, 8.1
 */
public class DBSRepositoryDescriptor {

    @XNode("@name")
    @XRegistryId
    public String name;

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    protected Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
    }

    @XNode("@headless")
    protected Boolean headless;

    /** @since 11.2 */
    public Boolean isHeadless() {
        return headless;
    }

    @XNode("idType")
    public String idType; // "varchar", "uuid", "sequence"

    protected FulltextDescriptor fulltextDescriptor = new FulltextDescriptor();

    public FulltextDescriptor getFulltextDescriptor() {
        return fulltextDescriptor;
    }

    @XNode("fulltext@fieldSizeLimit")
    public void setFulltextFieldSizeLimit(int fieldSizeLimit) {
        fulltextDescriptor.setFulltextFieldSizeLimit(fieldSizeLimit);
    }

    @XNode("fulltext@disabled")
    public void setFulltextDisabled(boolean disabled) {
        fulltextDescriptor.setFulltextDisabled(disabled);
    }

    /** @since 11.1 */
    @XNode("fulltext@storedInBlob")
    public void setFulltextStoredInBlob(boolean storedInBlob) {
        fulltextDescriptor.setFulltextStoredInBlob(storedInBlob);
    }

    @XNode("fulltext@searchDisabled")
    public void setFulltextSearchDisabled(boolean disabled) {
        fulltextDescriptor.setFulltextSearchDisabled(disabled);
    }

    @XNodeList(value = "fulltext/index", type = ArrayList.class, componentType = FulltextIndexDescriptor.class)
    public void setFulltextIndexes(List<FulltextIndexDescriptor> fulltextIndexes) {
        fulltextDescriptor.setFulltextIndexes(fulltextIndexes);
    }

    @XNodeList(value = "fulltext/excludedTypes/type", type = HashSet.class, componentType = String.class)
    public void setFulltextExcludedTypes(Set<String> fulltextExcludedTypes) {
        fulltextDescriptor.setFulltextExcludedTypes(fulltextExcludedTypes);
    }

    @XNodeList(value = "fulltext/includedTypes/type", type = HashSet.class, componentType = String.class)
    public void setFulltextIncludedTypes(Set<String> fulltextIncludedTypes) {
        fulltextDescriptor.setFulltextIncludedTypes(fulltextIncludedTypes);
    }

    /** @since 8.10 */
    @XNode("cache@enabled")
    private Boolean cacheEnabled;

    /** @since 8.10 */
    public boolean isCacheEnabled() {
        return isTrue(cacheEnabled);
    }

    /** @since 8.10 */
    protected void setCacheEnabled(boolean enabled) {
        cacheEnabled = Boolean.valueOf(enabled);
    }

    /** @since 8.10 */
    @XNode("cache@ttl")
    public Long cacheTTL;

    /** @since 8.10 */
    @XNode("cache@maxSize")
    public Long cacheMaxSize;

    /** @since 8.10 */
    @XNode("cache@concurrencyLevel")
    public Integer cacheConcurrencyLevel;

    /** @since 8.10 */
    @XNode("clustering/invalidatorClass")
    public Class<? extends DBSClusterInvalidator> clusterInvalidatorClass;

    /** @since 9.1 */
    @XNode("changeTokenEnabled")
    private Boolean changeTokenEnabled;

    /** @since 9.1 */
    public boolean isChangeTokenEnabled() {
        return isTrue(changeTokenEnabled);
    }

    /** @since 9.1 */
    public void setChangeTokenEnabled(boolean enabled) {
        this.changeTokenEnabled = Boolean.valueOf(enabled);
    }

    @XNode("pool")
    public PoolConfiguration pool;

    @XNode("createIndexes")
    protected Boolean createIndexes;

    /** @since 11.5 */
    public boolean isCreateIndexes() {
        return isNotFalse(createIndexes);
    }

    public void merge(DBSRepositoryDescriptor other) {
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
                pool = new PoolConfiguration(other.pool);
            } else {
                pool.merge(other.pool);
            }
        }
        if (other.idType != null) {
            idType = other.idType;
        }
        fulltextDescriptor.merge(other.fulltextDescriptor);
        if (other.cacheEnabled != null) {
            cacheEnabled = other.cacheEnabled;
        }
        if (other.cacheTTL != null) {
            cacheTTL = other.cacheTTL;
        }
        if (other.cacheMaxSize != null) {
            cacheMaxSize = other.cacheMaxSize;
        }
        if (other.cacheConcurrencyLevel != null) {
            cacheConcurrencyLevel = other.cacheConcurrencyLevel;
        }
        if (other.clusterInvalidatorClass != null) {
            clusterInvalidatorClass = other.clusterInvalidatorClass;
        }
        if (other.changeTokenEnabled != null) {
            changeTokenEnabled = other.changeTokenEnabled;
        }
    }
}
