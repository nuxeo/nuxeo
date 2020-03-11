/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.platform.routing.api;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
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

    public RouteFolderElement(DocumentRouteElement element, RouteTable table, boolean isFirstChild,
            RouteFolderElement parent, int depth) {
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
