/*
 * (C) Copyright 2010-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    mcedica
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.routing.api;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Wraps a documentElement adding informations about the level where the
 * document is inside the container documentRoute
 *
 * @author <a href="mailto:mcedica@nuxeo.com">Mariana Cedica</a>
 */
public class DocumentRouteTableElement {

    protected final DocumentRouteElement element;

    protected final RouteTable table;

    protected final int depth;

    protected RouteFolderElement parent;

    protected boolean isFirstChild;

    protected List<RouteFolderElement> firstChildList = new ArrayList<RouteFolderElement>();

    public DocumentRouteTableElement(DocumentRouteElement element,
            RouteTable table, int depth, RouteFolderElement parent,
            boolean isFirstChild) {
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
