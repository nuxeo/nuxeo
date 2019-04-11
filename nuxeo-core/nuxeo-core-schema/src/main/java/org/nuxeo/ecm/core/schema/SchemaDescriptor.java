/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.schema;

import java.util.LinkedHashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a reference to a schema from a document type or a facet.
 */
@XObject("schema")
public class SchemaDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@lazy")
    public boolean isLazy = true;

    public SchemaDescriptor() {
    }

    public SchemaDescriptor(String name) {
        this.name = name;
    }

    public static Set<String> getSchemaNames(SchemaDescriptor[] sds) {
        Set<String> set = new LinkedHashSet<>();
        for (SchemaDescriptor sd : sds) {
            set.add(sd.name);
        }
        return set;
    }

}
