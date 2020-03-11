/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.platform.routing.core.impl.jsongraph;

import java.util.Locale;

import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;

/**
 * @since 7.2
 */
public class NodeView {

    public int x;

    public int y;

    public boolean isForkNode;

    public boolean isStartNode;

    public boolean isEndNode;

    public String id;

    public String title;

    public String state;

    public boolean isMerge;

    public boolean isMultiTask;

    public boolean hasSubWorkflow;

    public NodeView(GraphNode node, Locale locale) {
        this.x = Integer.parseInt((String) node.getDocument().getPropertyValue(GraphNode.PROP_NODE_X_COORDINATE));
        this.y = Integer.parseInt((String) node.getDocument().getPropertyValue(GraphNode.PROP_NODE_Y_COORDINATE));
        this.isForkNode = node.isFork();
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

    /**
     * @since 11.1
     */
    public boolean isForkNode() {
        return isForkNode;
    }

    public boolean isStartNode() {
        return isStartNode;
    }
}
