/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.impl.jsongraph;

import java.util.Locale;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;

/**
 * @since 7.2
 */
public class NodeView {

    public int x;

    public int y;

    public boolean isStartNode;

    public boolean isEndNode;

    public String id;

    public String title;

    public String state;

    public boolean isMerge;

    public boolean isMultiTask;

    public boolean hasSubWorkflow;

    public NodeView(GraphNode node, Locale locale) throws ClientException {
        this.x = Integer.parseInt((String) node.getDocument().getPropertyValue(GraphNode.PROP_NODE_X_COORDINATE));
        this.y = Integer.parseInt((String) node.getDocument().getPropertyValue(GraphNode.PROP_NODE_Y_COORDINATE));
        this.isStartNode = node.isStart();
        this.isEndNode = node.isStop();
        this.id = node.getId();
        String titleProp = (String) node.getDocument().getPropertyValue(GraphNode.PROP_TITLE);
        this.title = JsonGraphRoute.getI18nLabel(titleProp, locale);
        this.state = node.getDocument().getCurrentLifeCycleState();
        this.isMerge = node.isMerge();
        this.isMultiTask = node.hasMultipleTasks();
        this.hasSubWorkflow = node.hasSubRoute();
    }

    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getTitle() {
        return title;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isEndNode() {
        return isEndNode;
    }

    public boolean isStartNode() {
        return isStartNode;
    }
}