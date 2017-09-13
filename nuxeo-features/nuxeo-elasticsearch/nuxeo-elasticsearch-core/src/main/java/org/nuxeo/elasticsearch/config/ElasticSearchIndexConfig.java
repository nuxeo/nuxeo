/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.elasticsearch.config;

import org.apache.commons.io.IOUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

import java.io.IOException;

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ALL_FIELDS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.BINARYTEXT_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;

/**
 * XMap descriptor for configuring an index
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject(value = "elasticSearchIndex")
public class ElasticSearchIndexConfig {
    final public static String DEFAULT_SETTING_FILE = "default-doc-settings.json";
    final public static String DEFAULT_MAPPING_FILE = "default-doc-mapping.json";

    @XNode("@enabled")
    protected boolean isEnabled = true;

    @Override
    public String toString() {
        if (isEnabled()) {
            return String.format("EsIndexConfig(%s, %s, %s)", getName(), getRepositoryName(), getType());
        }
        return "EsIndexConfig disabled";
    }

    @XNode("@name")
    protected String name;

    @XNode("@repository")
    protected String repositoryName;

    private static final String DEFAULT_REPOSITORY_NAME = "default";

    @XNode("@type")
    protected String type = DOC_TYPE;

    @XNode("@create")
    protected boolean create = true;

    @XNode("settings")
    protected String settings;

    @XNode("settings@file")
    protected String settingsFile;

    @XNode("mapping")
    protected String mapping;

    @XNode("mapping@file")
    protected String mappingFile;

    @XNodeList(value = "fetchFromSource/exclude", type = String[].class, componentType = String.class)
    protected String[] excludes;

    @XNodeList(value = "fetchFromSource/include", type = String[].class, componentType = String.class)
    protected String[] includes;

    public String[] getExcludes() {
        if (excludes == null) {
            return new String[]{BINARYTEXT_FIELD};
        }
        return excludes;
    }

    public String[] getIncludes() {
        if (includes == null || includes.length == 0) {
            return new String[]{ALL_FIELDS};
        }
        return includes;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSettings() {
        if (settingsFile != null) {
            return contentOfFile(settingsFile);
        } else if (settings != null && !settings.isEmpty()) {
            return settings;
        }
        return contentOfFile(DEFAULT_SETTING_FILE);
    }

    private String contentOfFile(String filename) {
        try {
            // getResourceAsStream is needed getResource will not work when called from another module
            return IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(filename), "UTF-8");
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not load resource file: " + filename, e);
        }
    }

    public String getMapping() {
        if (mappingFile != null) {
            return contentOfFile(mappingFile);
        } else if (mapping != null && !mapping.isEmpty()) {
            return mapping;
        }
        return contentOfFile(DEFAULT_MAPPING_FILE);
    }

    public boolean mustCreate() {
        return create;
    }

    public String getRepositoryName() {
        if (isDocumentIndex() && repositoryName == null) {
            repositoryName = DEFAULT_REPOSITORY_NAME;
        }
        return repositoryName;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * Return true if the index/mapping is associated with a Nuxeo document repository
     *
     * @since 7.4
     */
    public boolean isDocumentIndex() {
        return DOC_TYPE.equals(getType());
    }

    /**
     * Use {@code other} mapping and settings if not defined.
     */
    public void merge(final ElasticSearchIndexConfig other) {
        if (other == null) {
            return;
        }
        if (mapping == null && other.mapping != null) {
            mapping = other.mapping;
        }
        if (settings == null && other.settings != null) {
            settings = other.settings;
        }
        if (mappingFile == null && other.mappingFile != null) {
            mappingFile = other.mappingFile;
        }
        if (settingsFile == null && other.settingsFile != null) {
            settingsFile = other.settingsFile;
        }
    }

}
