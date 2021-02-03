/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap.registry;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * To be referenced in {@link SampleDescriptor}.
 *
 * @since 11.5
 */
@XObject("alias")
public class SampleDescriptorAlias {

    @XNode("@name")
    public String name;

    @XNode("description")
    public String description;

    @XNodeList(value = "stringList/item", type = ArrayList.class, componentType = String.class)
    public List<String> stringList;

    // needed by xmap
    public SampleDescriptorAlias() {
    }

    // needed by tests
    public SampleDescriptorAlias(String name, String description) {
        this(name, description, null);
    }

    // needed by tests
    public SampleDescriptorAlias(String name, String description, List<String> stringList) {
        this.name = name;
        this.description = description;
        this.stringList = new ArrayList<>();
        if (stringList != null) {
            this.stringList.addAll(stringList);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, false);
    }

    @Override
    public String toString() {
        return String.format("SampleDescriptorAlias{name=%s,description=%s,stringList=%s}", name, description,
                stringList);
    }

}
