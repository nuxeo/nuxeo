/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    mcedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.routing.api;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Wraps a documentElement adding informations about the level where the document is inside the container documentRoute
 *
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class DocumentRouteTableElement {

    protected final DocumentRouteElement element;

    protected final RouteTable table;

    protected final int depth;

    protected RouteFolderElement parent;

    protected boolean isFirstChild;

    protected List<RouteFolderElement> firstChildList = new ArrayList<>();

    public DocumentRouteTableElement(DocumentRouteElement element, RouteTable table, int depth,
            RouteFolderElement parent, boolean isFirstChild) {
        this.table = table;
        this.depth = depth;
        this.element = element;
        this.parent = parent;
        this.isFirstChild = isFirstChild;
    }

    public RouteFolderElement getParent() {
        return parent;
    }

    public DocumentRouteElement getElement() {
        return element;
    }

    public int getDepth() {
        return depth;
    }

    public RouteTable getRouteTable() {
        return table;
    }

    public List<RouteFolderElement> getFirstChildFolders() {
        return firstChildList;
    }

    public int getRouteMaxDepth() {
        return table.getMaxDepth();
    }

    public DocumentModel getDocument() {
        return element.getDocument();
    }

    public void computeFirstChildList() {
        RouteFolderElement currentParent = parent;
        boolean currentIsFirst = isFirstChild;
        while (currentIsFirst && currentParent != null) {
            currentIsFirst = currentParent.isFirstChild;
            firstChildList.add(0, currentParent);
            currentParent = currentParent.parent;
        }
    }
}
