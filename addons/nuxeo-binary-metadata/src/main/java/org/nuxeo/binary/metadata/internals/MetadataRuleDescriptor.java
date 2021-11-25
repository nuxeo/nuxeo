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

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.1
 */
@XObject("rule")
public class MetadataRuleDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@order")
    protected Integer order;

    @XNode("@enabled")
    protected Boolean enabled;

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

    /**
     * @since 2021.13
     */
    public boolean isAsync() {
        return isTrue(isAsync);
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

}
