/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.io.Serializable;

/**
 * Property hierarchy node.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class PropertyHierarchyNode implements Serializable {

    private static final long serialVersionUID = -5497891597767526856L;

    private String nodeType;

    private String nodeValue;

    /**
     * Instantiates a new property hierarchy node.
     * 
     * @param nodeType the node type
     * @param nodeValue the node value
     */
    public PropertyHierarchyNode(String nodeType, String nodeValue) {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(String nodeValue) {
        this.nodeValue = nodeValue;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("{");
        sb.append(nodeType);
        sb.append(",");
        sb.append(nodeValue);
        sb.append("}");
        return sb.toString();
    }
}
