/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @XNodeList(value = "schema", type = SchemaDescriptor[].class, componentType = SchemaDescriptor.class)
    public SchemaDescriptor[] schemas;

    /* empty constructor needed by XMap */
    public FacetDescriptor() {
    }

    public FacetDescriptor(String name, SchemaDescriptor[] schemas) {
        this.name = name;
        this.schemas = schemas == null ? new SchemaDescriptor[0] : schemas;
    }

    @Override
    public String toString() {
        return "Facet(" + name + ',' + SchemaDescriptor.getSchemaNames(schemas)
                + ')';
    }

}
