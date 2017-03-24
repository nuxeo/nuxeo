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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Facet Descriptor.
 */
@XObject("facet")
public class FacetDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@perDocumentQuery")
    public Boolean perDocumentQuery;

    @XNodeList(value = "schema", type = SchemaDescriptor[].class, componentType = SchemaDescriptor.class)
    public SchemaDescriptor[] schemas;

    /* empty constructor needed by XMap */
    public FacetDescriptor() {
    }

    public FacetDescriptor(String name, SchemaDescriptor[] schemas) {
        this.name = name;
        this.schemas = schemas == null ? new SchemaDescriptor[0] : schemas;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Facet(" + name + ',' + SchemaDescriptor.getSchemaNames(schemas) + ')';
    }

}
