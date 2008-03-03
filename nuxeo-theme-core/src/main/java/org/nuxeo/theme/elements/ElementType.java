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

package org.nuxeo.theme.elements;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.nodes.NodeTypeFamily;
import org.nuxeo.theme.types.Type;
import org.nuxeo.theme.types.TypeFamily;

@XObject("element")
public final class ElementType implements Type {

    @XNode("@name")
    public String name;

    @XNode("class")
    public String className;

    @XNode("node-type")
    public String nodeTypeName;

    public ElementType() {
    }

    public ElementType(String name, String className, String nodeTypeName) {
        this.name = name;
        this.nodeTypeName = nodeTypeName;
        this.className = className;
    }

    public NodeTypeFamily getNodeTypeFamily() {
        if (nodeTypeName.equals("leaf")) {
            return NodeTypeFamily.LEAF;
        } else if (nodeTypeName.equals("inner")) {
            return NodeTypeFamily.INNER;
        }
        return null;
    }

    public String getTypeName() {
        return name;
    }

    public TypeFamily getTypeFamily() {
        return TypeFamily.ELEMENT;
    }

    public String getClassName() {
        return className;
    }

}
