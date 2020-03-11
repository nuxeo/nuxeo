/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Repository proxies configuration descriptor.
 */
@XObject("proxies")
public class ProxiesDescriptor {

    @XNode("@type")
    private String type;

    @XNodeList(value = "schema@name", type = HashSet.class, componentType = String.class)
    private Set<String> schemas = new HashSet<>(0);

    /* empty constructor needed by XMap */
    public ProxiesDescriptor() {
    }

    public String getType() {
        return type == null ? "*" : type;
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(type=" + getType() + ", schemas=" + getSchemas() + ")";
    }

}
