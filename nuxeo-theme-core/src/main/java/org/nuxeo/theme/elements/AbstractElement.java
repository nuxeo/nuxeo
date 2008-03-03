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

import java.net.URL;
import java.util.List;

import org.nuxeo.theme.nodes.AbstractNode;
import org.nuxeo.theme.nodes.Node;
import org.nuxeo.theme.nodes.NodeTypeFamily;

public abstract class AbstractElement extends AbstractNode implements Element {

    private ElementType elementType;

    private Integer uid;

    private String name;

    private String description;

    public Integer getUid() {
        return uid;
    }

    public void setUid(final Integer uid) {
        this.uid = uid;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public void setElementType(final ElementType elementType) {
        this.elementType = elementType;
    }

    @Override
    public NodeTypeFamily getNodeTypeFamily() {
        return elementType.getNodeTypeFamily();
    }

    public String hash() {
        if (uid == null) {
            return null;
        }
        return uid.toString();
    }

    public List<Node> getChildrenInContext(final URL themeURL) {
        return getChildren();
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isEmpty() {
        return !hasChildren();
    }

    public String computeXPath() {
        final StringBuilder s = new StringBuilder();
        String typeName = null;
        Element e = this;
        do {
            Integer order = e.getOrder();
            if (order != null) {
                order += 1;
                s.insert(0, "[" + order + ']');
            }
            typeName = e.getElementType().getTypeName();
            if (typeName.equals("theme")) {
                break;
            }
            s.insert(0, typeName);
            if (typeName.equals("page")) {
                break;
            }
            s.insert(0, '/');
            e = (Element) e.getParent();
        } while (e != null);
        return s.toString();
    }

}
