/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.elasticsearch.config;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.elasticsearch.api.ESClientFactory;

/**
 * @since 9.3
 */
@XObject(value = "elasticSearchClient")
public class ElasticSearchClientConfig {

    @XNode("@class")
    protected Class<ESClientFactory> klass;

    @XNode("@useExternalVersion")
    protected boolean externalVersion = true;

    @XNodeMap(value = "option", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> options = new HashMap<>();

    public Class<ESClientFactory> getKlass() {
        return klass;
    }

    public boolean useExternalVersion() {
        return externalVersion;
    }

    public String getOption(String key) {
        return options.get(key);
    }

    public String getOption(String key, String defaultValue) {
        return options.getOrDefault(key, defaultValue);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ElasticSearchClientConfig{");
        sb.append("options=").append(options);
        sb.append('}');
        return sb.toString();
    }
}
