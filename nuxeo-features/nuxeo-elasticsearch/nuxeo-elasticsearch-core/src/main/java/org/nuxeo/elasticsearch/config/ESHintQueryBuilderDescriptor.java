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

import static java.util.Objects.requireNonNull;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESHintQueryBuilder;
import org.nuxeo.runtime.model.Descriptor;

/**
 * This descriptor allows to add a Elasticsearch Hint.
 *
 * @since 11.1
 */
@XObject("hint")
public class ESHintQueryBuilderDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@class")
    protected Class<? extends ESHintQueryBuilder> klass;

    @XNode("@remove")
    public boolean remove;

    @Override
    public String getId() {
        return name;
    }

    @Override
    public ESHintQueryBuilderDescriptor merge(Descriptor descriptor) {
        ESHintQueryBuilderDescriptor other = ESHintQueryBuilderDescriptor.class.cast(descriptor);
        ESHintQueryBuilderDescriptor merged = new ESHintQueryBuilderDescriptor();
        merged.name = requireNonNull(other.name != null ? other.name : name);
        merged.klass = requireNonNull(other.klass != null ? other.klass : klass);
        merged.remove = other.remove;
        return merged;
    }

    @Override
    public boolean doesRemove() {
        return remove;
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
