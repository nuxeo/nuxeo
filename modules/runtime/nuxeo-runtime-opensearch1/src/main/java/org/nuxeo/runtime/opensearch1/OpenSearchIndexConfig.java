/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.runtime.opensearch1;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.nuxeo.runtime.model.Descriptor.getIfEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * XMap descriptor for configuring an index
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject(value = "index")
public class OpenSearchIndexConfig implements Descriptor {

    public static final String DEFAULT_SETTING_FILE = "default-settings.json";

    public static final String DEFAULT_MAPPING_FILE = "default-mapping.json";

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled;

    @XNode("@create")
    protected Boolean create;

    @XNodeList(value = "client@id", type = ArrayList.class, componentType = String.class)
    protected List<String> clientIds;

    protected String settingsContent;

    protected String mappingContent;

    // @since 2021.17
    protected List<String> extraMappingContents = new ArrayList<>();

    // @since 2021.17
    @XNode("mapping@append")
    protected boolean mappingAppend;

    @Override
    public String getId() {
        return name;
    }

    /**
     * @return the index name
     */
    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return BooleanUtils.toBooleanDefaultIfNull(enabled, true);
    }

    public boolean mustCreate() {
        return BooleanUtils.toBooleanDefaultIfNull(create, true);
    }

    public List<String> getClientIds() {
        return Collections.unmodifiableList(clientIds);
    }

    public String getSettingsContent() {
        return Objects.requireNonNullElseGet(settingsContent, () -> contentOfFile(DEFAULT_SETTING_FILE));
    }

    @XNode("settings")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setSettings(String settings) {
        // handle the empty element when file tag is used
        if (StringUtils.isNotBlank(settings)) {
            this.settingsContent = settings;
        }
    }

    @XNode("settings@file")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setSettingsFile(String settingsFile) {
        this.settingsContent = contentOfFile(settingsFile);
    }

    public String getMappingContent() {
        return Objects.requireNonNullElseGet(mappingContent, () -> contentOfFile(DEFAULT_MAPPING_FILE));
    }

    @XNode("mapping")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setMapping(String mapping) {
        // handle the empty element when file tag is used
        if (StringUtils.isNotBlank(mapping)) {
            this.mappingContent = mapping;
        }
    }

    @XNode("mapping@file")
    @SuppressWarnings("unused") // used by X framework when unmarshalling
    protected void setMappingFile(String mappingFile) {
        // TODO review this was the previous behavior, the wrong file name triggered the error when putting mapping
        this.mappingContent = mappingFile.endsWith(".json") ? contentOfFile(mappingFile) : mappingFile;
    }

    public List<String> getExtraMappingContents() {
        return Collections.unmodifiableList(extraMappingContents);
    }

    protected String contentOfFile(String filename) {
        try (InputStream stream = getResourceStream(filename)) {
            return IOUtils.toString(stream, UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load resource file: " + filename, e);
        }
    }

    protected InputStream getResourceStream(String filename) {
        // First check if the resource is available on the config directory
        File file = new File(Environment.getDefault().getConfig(), filename);
        if (file.exists()) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                // try another way
            }
        }

        // getResourceAsStream is needed getResource will not work when called from another module
        InputStream ret = this.getClass().getClassLoader().getResourceAsStream(filename);
        if (ret == null) {
            throw new IllegalArgumentException(
                    String.format("Resource file cannot be found: %s or %s", file.getAbsolutePath(), filename));
        }
        return ret;
    }

    /**
     * Use {@code other} mapping and settings if not defined.
     */
    public OpenSearchIndexConfig merge(Descriptor o) {
        var other = (OpenSearchIndexConfig) o;
        var merged = new OpenSearchIndexConfig();
        merged.name = getIfNull(other.name, name);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.create = getIfNull(other.create, create);
        merged.clientIds = getIfEmpty(other.clientIds, clientIds);
        merged.settingsContent = getIfNull(other.settingsContent, settingsContent);
        // propagate extraMappings only if descriptor append mapping
        if (other.mappingAppend) {
            merged.extraMappingContents.addAll(other.extraMappingContents);
            merged.extraMappingContents.addAll(extraMappingContents);
            merged.extraMappingContents.add(other.mappingContent);
            merged.mappingContent = mappingContent;
        } else {
            merged.mappingContent = getIfNull(other.mappingContent, mappingContent);
        }
        return merged;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toStringInclude(this, "name", "clientIds", "create", "enabled");
    }
}
