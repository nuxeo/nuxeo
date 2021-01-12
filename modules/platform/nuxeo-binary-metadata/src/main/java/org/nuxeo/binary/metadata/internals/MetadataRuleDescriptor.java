/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @since 7.1
 */
@XObject("rule")
@XRegistry(enable = false, compatWarnOnMerge = true)
public class MetadataRuleDescriptor implements Comparable<MetadataRuleDescriptor> {

    /** @since 11.5 **/
    public static final Comparator<MetadataRuleDescriptor> COMPARATOR = Comparator.comparing(
            MetadataRuleDescriptor::getOrder).thenComparing(MetadataRuleDescriptor::getId);

    @XNode("@id")
    @XRegistryId
    protected String id;

    @XNode(value = "@order", defaultAssignment = "0")
    protected Integer order;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean enabled;

    @XNode("@async")
    protected Boolean isAsync;

    @XNodeList(value = "metadataMappings/metadataMapping-id", componentType = String.class, type = ArrayList.class)
    protected List<String> metadataMappingIdDescriptors;

    @XNodeList(value = "filters/filter-id", type = ArrayList.class, componentType = String.class)
    protected List<String> filterIds;

    public String getId() {
        return id;
    }

    public Integer getOrder() {
        return order;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getIsAsync() {
        return isAsync;
    }

    public List<String> getMetadataMappingIdDescriptors() {
        return metadataMappingIdDescriptors;
    }

    public List<String> getFilterIds() {
        return filterIds;
    }

    @Override
    public String toString() {
        return "MetadataRuleDescriptor [id=" + id + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MetadataRuleDescriptor other = (MetadataRuleDescriptor) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(MetadataRuleDescriptor o) {
        return COMPARATOR.compare(this, o);
    }

}
