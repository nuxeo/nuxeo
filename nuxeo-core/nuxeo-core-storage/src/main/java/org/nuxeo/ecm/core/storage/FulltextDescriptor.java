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
package org.nuxeo.ecm.core.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Structure holding fulltext descriptor info for generic fulltext indexing.
 * <p>
 * Not directly a XObject, but used by various RepositoryDescriptors.
 *
 * @since 7.10-HF04, 8.1
 */
public class FulltextDescriptor {

    @XObject(value = "index")
    public static class FulltextIndexDescriptor {

        @XNode("@name")
        public String name;

        /** string or blob */
        @XNode("fieldType")
        public String fieldType;

        @XNodeList(value = "field", type = HashSet.class, componentType = String.class)
        public Set<String> fields = new HashSet<>(0);

        @XNodeList(value = "excludeField", type = HashSet.class, componentType = String.class)
        public Set<String> excludeFields = new HashSet<>(0);

        public FulltextIndexDescriptor() {
        }

        /** Copy constructor. */
        public FulltextIndexDescriptor(FulltextIndexDescriptor other) {
            name = other.name;
            fieldType = other.fieldType;
            fields = new HashSet<>(other.fields);
            excludeFields = new HashSet<>(other.excludeFields);
        }

        public static List<FulltextIndexDescriptor> copyList(List<FulltextIndexDescriptor> other) {
            List<FulltextIndexDescriptor> copy = new ArrayList<>(other.size());
            for (FulltextIndexDescriptor fid : other) {
                copy.add(new FulltextIndexDescriptor(fid));
            }
            return copy;
        }

        public void merge(FulltextIndexDescriptor other) {
            if (other.name != null) {
                name = other.name;
            }
            if (other.fieldType != null) {
                fieldType = other.fieldType;
            }
            fields.addAll(other.fields);
            excludeFields.addAll(other.excludeFields);
        }
    }

    public static final int FULLTEXT_FIELD_SIZE_LIMIT_DEFAULT = 128 * 1024; // 128 K

    private Integer fulltextFieldSizeLimit;

    public int getFulltextFieldSizeLimit() {
        return fulltextFieldSizeLimit == null ? FULLTEXT_FIELD_SIZE_LIMIT_DEFAULT : fulltextFieldSizeLimit.intValue();
    }

    public void setFulltextFieldSizeLimit(int fulltextFieldSizeLimit) {
        this.fulltextFieldSizeLimit = Integer.valueOf(fulltextFieldSizeLimit);
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    private Boolean fulltextDisabled;

    public boolean getFulltextDisabled() {
        return defaultFalse(fulltextDisabled);
    }

    public void setFulltextDisabled(boolean disabled) {
        fulltextDisabled = Boolean.valueOf(disabled);
    }

    private Boolean fulltextSearchDisabled;

    public boolean getFulltextSearchDisabled() {
        if (getFulltextDisabled()) {
            return true;
        }
        return defaultFalse(fulltextSearchDisabled);
    }

    public void setFulltextSearchDisabled(boolean disabled) {
        fulltextSearchDisabled = Boolean.valueOf(disabled);
    }

    private List<FulltextIndexDescriptor> fulltextIndexes = new ArrayList<>(0);

    public List<FulltextIndexDescriptor> getFulltextIndexes() {
        return fulltextIndexes;
    }

    public void setFulltextIndexes(List<FulltextIndexDescriptor> fulltextIndexes) {
        this.fulltextIndexes = fulltextIndexes;
    }

    private Set<String> fulltextExcludedTypes = new HashSet<>(0);

    public Set<String> getFulltextExcludedTypes() {
        return fulltextExcludedTypes;
    }

    public void setFulltextExcludedTypes(Set<String> fulltextExcludedTypes) {
        this.fulltextExcludedTypes = fulltextExcludedTypes;
    }

    private Set<String> fulltextIncludedTypes = new HashSet<>(0);

    public Set<String> getFulltextIncludedTypes() {
        return fulltextIncludedTypes;
    }

    public void setFulltextIncludedTypes(Set<String> fulltextIncludedTypes) {
        this.fulltextIncludedTypes = fulltextIncludedTypes;
    }

    public FulltextDescriptor() {
    }

    /** Copy constructor. */
    public FulltextDescriptor(FulltextDescriptor other) {
        fulltextFieldSizeLimit = other.fulltextFieldSizeLimit;
        fulltextDisabled = other.fulltextDisabled;
        fulltextSearchDisabled = other.fulltextSearchDisabled;
        fulltextIndexes = FulltextIndexDescriptor.copyList(other.fulltextIndexes);
        fulltextExcludedTypes = new HashSet<>(other.fulltextExcludedTypes);
        fulltextIncludedTypes = new HashSet<>(other.fulltextIncludedTypes);
    }

    public void merge(FulltextDescriptor other) {
        if (other.fulltextFieldSizeLimit != null) {
            fulltextFieldSizeLimit = other.fulltextFieldSizeLimit;
        }
        if (other.fulltextDisabled != null) {
            fulltextDisabled = other.fulltextDisabled;
        }
        if (other.fulltextSearchDisabled != null) {
            fulltextSearchDisabled = other.fulltextSearchDisabled;
        }
        for (FulltextIndexDescriptor oi : other.fulltextIndexes) {
            boolean append = true;
            for (FulltextIndexDescriptor i : fulltextIndexes) {
                if (Objects.equals(i.name, oi.name)) {
                    i.merge(oi);
                    append = false;
                    break;
                }
            }
            if (append) {
                fulltextIndexes.add(oi);
            }
        }
        fulltextExcludedTypes.addAll(other.fulltextExcludedTypes);
        fulltextIncludedTypes.addAll(other.fulltextIncludedTypes);
    }

}
