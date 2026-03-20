/*
 * (C) Copyright 2024-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

import static org.apache.commons.lang3.BooleanUtils.toBooleanDefaultIfNull;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter;
import org.nuxeo.ecm.core.search.index.IndexingJsonWriter;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since 2025.0
 */
@XObject("searchIndex")
public class SearchIndexDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@default")
    protected Boolean isDefault;

    @XNode("@searchClient")
    protected String client;

    @XNode("@repository")
    protected String repository;

    @XNode("@writerClass")
    protected Class<? extends IndexingJsonWriter> writerClass;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return toBooleanDefaultIfNull(enabled, true);
    }

    public boolean isDefault() {
        return toBooleanDefaultIfNull(isDefault, false);
    }

    public String getClient() {
        return client;
    }

    public String getRepositoryName() {
        return repository;
    }

    public Class<? extends IndexingJsonWriter> getWriterClass() {
        return getIfNull(writerClass, DefaultIndexingJsonWriter.class);
    }

    public IndexingJsonWriter newWriterInstance() {
        try {
            return getWriterClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Invalid JsonWriter class: " + writerClass + " for: " + name, e);
        }
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (SearchIndexDescriptor) o;
        var merged = new SearchIndexDescriptor();
        merged.name = name; // we merge based on name, so no name merging needed
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.isDefault = getIfNull(other.isDefault, isDefault);
        merged.client = defaultIfBlank(other.client, client);
        merged.repository = defaultIfBlank(other.repository, repository);
        merged.writerClass = getIfNull(other.writerClass, writerClass);
        return merged;
    }
}
