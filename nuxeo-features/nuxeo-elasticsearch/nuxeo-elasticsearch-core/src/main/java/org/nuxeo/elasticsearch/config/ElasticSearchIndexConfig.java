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

import static org.nuxeo.elasticsearch.ElasticSearchConstants.ALL_FIELDS;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.BINARYTEXT_FIELD;
import static org.nuxeo.elasticsearch.ElasticSearchConstants.DOC_TYPE;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for configuring an index
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@XObject(value = "elasticSearchIndex")
public class ElasticSearchIndexConfig {

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

    final public static String DEFAULT_SETTING = "{\n" //
            + "   \"number_of_shards\" : 1,\n" //
            + "   \"number_of_replicas\" : 0,\n" //
            + "   \"analysis\" : {\n" //
            + "      \"filter\" : {\n" //
            + "         \"truncate_filter\" : {\n" //
            + "            \"length\" : 256,\n" //
            + "            \"type\" : \"truncate\"\n" //
            + "         },\n" //
            + "         \"en_stem_filter\" : {\n" //
            + "            \"name\" : \"minimal_english\",\n" //
            + "            \"type\" : \"stemmer\"\n" //
            + "         },\n" //
            + "         \"en_stop_filter\" : {\n" //
            + "            \"stopwords\" : [\n" //
            + "               \"_english_\"\n" //
            + "            ],\n" //
            + "            \"type\" : \"stop\"\n" //
            + "         }\n" //
            + "      },\n" //
            + "      \"tokenizer\" : {\n" //
            + "         \"path_tokenizer\" : {\n" //
            + "            \"delimiter\" : \"/\",\n" //
            + "            \"type\" : \"path_hierarchy\"\n" //
            + "         }\n" + "      },\n" //
            + "      \"analyzer\" : {\n" //
            + "         \"en_analyzer\" : {\n" //
            + "            \"alias\" : \"fulltext\",\n" //
            + "            \"filter\" : [\n" //
            + "               \"lowercase\",\n" //
            + "               \"en_stop_filter\",\n" //
            + "               \"en_stem_filter\",\n" //
            + "               \"asciifolding\"\n" //
            + "            ],\n" //
            + "            \"type\" : \"custom\",\n" //
            + "            \"tokenizer\" : \"standard\"\n" //
            + "         },\n" //
            + "         \"path_analyzer\" : {\n" //
            + "            \"type\" : \"custom\",\n" //
            + "            \"tokenizer\" : \"path_tokenizer\"\n" //
            + "         },\n" //
            + "         \"default\" : {\n" //
            + "            \"type\" : \"custom\",\n" //
            + "            \"tokenizer\" : \"keyword\",\n" //
            + "            \"filter\" : [\n" //
            + "               \"truncate_filter\"\n" //
            + "            ]\n" //
            + "         }\n" //
            + "      }\n" //
            + "   }\n" //
            + "}";

    @XNode("mapping")
    protected String mapping;

    final public static String DEFAULT_MAPPING = "{\n" //
            + "   \"_all\" : {\n" //
            + "      \"analyzer\" : \"fulltext\"\n" //
            + "   },\n" //
            + "   \"properties\" : {\n" //
            + "      \"dc:title\" : {\n" //
            + "         \"type\" : \"multi_field\",\n" //
            + "         \"fields\" : {\n" //
            + "           \"dc:title\" : {\n" //
            + "             \"type\" : \"string\"\n" //
            + "           },\n" //
            + "           \"fulltext\" : {\n" //
            + "             \"boost\": 2,\n" //
            + "             \"type\": \"string\",\n" //
            + "             \"analyzer\" : \"fulltext\"\n" //
            + "          }\n" //
            + "        }\n" //
            + "      },\n" //
            + "      \"dc:description\" : {\n" //
            + "         \"type\" : \"multi_field\",\n" //
            + "         \"fields\" : {\n" //
            + "           \"dc:description\" : {\n" //
            + "             \"type\" : \"string\"\n" //
            + "           },\n" //
            + "           \"fulltext\" : {\n" //
            + "             \"boost\": 1.5,\n" //
            + "             \"type\": \"string\",\n" //
            + "             \"analyzer\" : \"fulltext\"\n" //
            + "          }\n" //
            + "        }\n" //
            + "      },\n" //
            + "      \"ecm:binarytext\" : {\n" //
            + "         \"type\" : \"string\",\n" //
            + "         \"index\" : \"no\",\n" //
            + "         \"include_in_all\" : true\n" //
            + "      },\n" //
            + "      \"ecm:path\" : {\n" //
            + "         \"type\" : \"multi_field\",\n" //
            + "         \"fields\" : {\n" //
            + "            \"children\" : {\n" //
            + "               \"analyzer\" : \"path_analyzer\",\n" //
            + "               \"search_analyzer\" : \"keyword\",\n" //
            + "               \"type\" : \"string\"\n" //
            + "            },\n" //
            + "            \"ecm:path\" : {\n" //
            + "               \"index\" : \"not_analyzed\",\n" //
            + "               \"type\" : \"string\"\n" //
            + "            }\n" //
            + "         }\n" //
            + "      },\n" //
            + "      \"dc:created\": {\n" //
            + "         \"format\": \"dateOptionalTime\",\n" //
            + "        \"type\": \"date\"\n" //
            + "      },\n" //
            + "      \"dc:modified\": {\n" //
            + "         \"format\": \"dateOptionalTime\",\n" //
            + "        \"type\": \"date\"\n" //
            + "      },\n" //
            + "      \"ecm:pos*\" : {\n" //
            + "         \"type\" : \"integer\"\n" //
            + "      }\n" //
            + "   }\n" //
            + "}";

    @XNodeList(value = "fetchFromSource/exclude", type = String[].class, componentType = String.class)
    protected String[] excludes;

    @XNodeList(value = "fetchFromSource/include", type = String[].class, componentType = String.class)
    protected String[] includes;

    public String[] getExcludes() {
        if (excludes == null) {
            return new String[] { BINARYTEXT_FIELD };
        }
        return excludes;
    }

    public String[] getIncludes() {
        if (includes == null || includes.length == 0) {
            return new String[] { ALL_FIELDS };
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
        return settings == null ? DEFAULT_SETTING : settings;
    }

    public String getMapping() {
        return mapping == null ? DEFAULT_MAPPING : mapping;
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
    }

}
