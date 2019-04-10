/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
