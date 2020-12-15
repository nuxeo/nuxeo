/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.elasticsearch.config;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;

/**
 * This descriptor allows to add a Elasticsearch Hint.
 *
 * @since 11.1
 */
@XObject("hint")
@XRegistry
public class ESHintQueryBuilderDescriptor {

    @XNode("@name")
    @XRegistryId
    protected String name;

    @XNode("@class")
    protected Class<? extends ESHintQueryBuilder> klass;

    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Class<? extends ESHintQueryBuilder> getKlass() {
        return klass;
    }

    public ESHintQueryBuilder newInstance() {
        try {
            return getKlass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }
}
