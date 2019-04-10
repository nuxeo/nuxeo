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

import java.util.List;
import java.util.Locale;

import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Transition;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode.Point;

/**
 * @since 7.2
 */
public class TransitionView {

    public String nodeSourceId;

    public String nodeTargetId;

    public String label;

    public List<Point> path;

    public TransitionView(String nodeSourceId, String nodeTargetId, Transition transition, Locale locale) {
        this.nodeSourceId = nodeSourceId;
        this.nodeTargetId = nodeTargetId;
        this.label = JsonGraphRoute.getI18nLabel(transition.getLabel(), locale);
        this.path = transition.getPath();
    }

    public String getLabel() {
        return label;
    }

    public String getNodeSourceId() {
        return nodeSourceId;
    }

    public String getNodeTargetId() {
        return nodeTargetId;
    }

    public List<Point> getPath() {
        return path;
    }
}
