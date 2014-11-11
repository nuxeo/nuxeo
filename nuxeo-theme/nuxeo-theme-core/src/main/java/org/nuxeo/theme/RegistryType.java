/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("registry")
public class RegistryType implements Type {

    @XNode("@name")
    public String name;

    @XNode("class")
    public String className;

    public RegistryType() {
    }

    public RegistryType(final String name, final String className) {
        this.name = name;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.REGISTRY;
    }

    public String getTypeName() {
        return "registry";
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
