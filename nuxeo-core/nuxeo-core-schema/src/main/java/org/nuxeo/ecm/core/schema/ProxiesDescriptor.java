/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
    private Set<String> schemas = new HashSet<String>(0);

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
        return getClass().getSimpleName() + "(type=" + getType() + ", schemas="
                + getSchemas() + ")";
    }

}
