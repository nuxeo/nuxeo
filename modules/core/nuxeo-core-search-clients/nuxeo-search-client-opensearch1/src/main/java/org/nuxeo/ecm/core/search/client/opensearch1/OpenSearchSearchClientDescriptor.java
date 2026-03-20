/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.0
 */
@XObject("searchClient")
public class OpenSearchSearchClientDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@clientId")
    protected String clientId;

    @XNodeMap(value = "searchIndex", key = "@name", type = HashMap.class, componentType = SearchIndex.class)
    protected Map<String, SearchIndex> searchIndexes;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    public Map<String, SearchIndex> getSearchIndexes() {
        return searchIndexes;
    }

    public String getClientId() {
        return getIfNull(clientId, "search/" + name);
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (OpenSearchSearchClientDescriptor) o;
        var merged = new OpenSearchSearchClientDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.clientId = defaultIfBlank(other.clientId, clientId);
        merged.searchIndexes = new HashMap<>(searchIndexes);
        other.searchIndexes.forEach((k, v) -> merged.searchIndexes.merge(k, v, SearchIndex::merge));
        return merged;
    }

    @XObject("searchIndex")
    public static final class SearchIndex implements Descriptor {

        @XNode("@name")
        protected String name;

        @XNode("@technicalName")
        protected String technicalName;

        @Override
        public String getId() {
            return name;
        }

        public String getTechnicalName() {
            return technicalName;
        }

        @Override
        public SearchIndex merge(Descriptor other) {
            return (SearchIndex) other;
        }
    }
}
