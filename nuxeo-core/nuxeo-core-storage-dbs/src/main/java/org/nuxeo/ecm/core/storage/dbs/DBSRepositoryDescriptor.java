/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.FulltextDescriptor.FulltextIndexDescriptor;

/**
 * DBS Repository Descriptor.
 *
 * @since 7.10-HF04, 8.1
 */
public class DBSRepositoryDescriptor implements Cloneable {

    public DBSRepositoryDescriptor() {
    }

    @XNode("@name")
    public String name;

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    protected Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
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

    @XNode("fulltext@searchDisabled")
    public void setFulltextSearchDisabled(boolean disabled) {
        fulltextDescriptor.setFulltextSearchDisabled(disabled);
    }

    @XNode("fulltext@parser")
    public void setFulltextParser(String fulltextParser) {
        fulltextDescriptor.setFulltextParser(fulltextParser);
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

    @Override
    public DBSRepositoryDescriptor clone() {
        try {
            DBSRepositoryDescriptor clone = (DBSRepositoryDescriptor) super.clone();
            clone.fulltextDescriptor = new FulltextDescriptor(fulltextDescriptor);
            return clone;
        } catch (CloneNotSupportedException e) { // cannot happen
            throw new RuntimeException(e);
        }
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
        if (other.idType != null) {
            idType = other.idType;
        }
        fulltextDescriptor.merge(other.fulltextDescriptor);
    }

}
