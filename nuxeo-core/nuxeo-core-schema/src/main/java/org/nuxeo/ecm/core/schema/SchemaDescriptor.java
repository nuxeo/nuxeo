/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        Set<String> set = new LinkedHashSet<String>();
        for (SchemaDescriptor sd : sds) {
            set.add(sd.name);
        }
        return set;
    }

}
