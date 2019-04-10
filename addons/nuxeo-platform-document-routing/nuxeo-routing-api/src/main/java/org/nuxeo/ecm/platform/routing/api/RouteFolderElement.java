/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 * @deprecated since 5.9.2 - Use only routes of type 'graph'
 */
@Deprecated
public class RouteFolderElement {
    protected DocumentRouteElement element;

    protected RouteTable table;

    protected boolean isFirstChild;

    protected RouteFolderElement parent;

    protected int totalChildCount;

    protected int depth;

    public RouteFolderElement(DocumentRouteElement element, RouteTable table,
            boolean isFirstChild, RouteFolderElement parent, int depth) {
        this.table = table;
        this.element = element;
        this.isFirstChild = isFirstChild;
        this.parent = parent;
        this.depth = depth;
    }

    public int getTotalChildCount() {
        return totalChildCount;
    }

    public void increaseTotalChildCount() {
        if (parent != null) {
            parent.increaseTotalChildCount();
        } else {
            table.increaseTotalChildCount();
        }
        totalChildCount++;
    }

    public DocumentRouteElement getRouteElement() {
        return element;
    }

    public int getDepth() {
        return depth;
    }
}
