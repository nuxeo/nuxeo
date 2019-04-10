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
 */
public class RouteTable {

    protected DocumentRoute route;

    protected int totalChildCount = 0;

    protected int maxDepth = 0;

    /**
     * @param routeElementDocument
     */
    public RouteTable(DocumentRoute route) {
        this.route = route;
    }

    /**
     * @return
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    public void increaseTotalChildCount() {
        totalChildCount++;
    }

    public int getTotalChildCount() {
        return totalChildCount;
    }

    /**
     * @param maxDepth
     */
    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

}
