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

package org.nuxeo.ecm.platform.routing.core.io;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;

/**
 * @since 7.2
 */
public class TaskCompletionRequest {

    private String comment;

    private Map<String, Serializable> variables;

    private boolean badJsonFormat;

    public TaskCompletionRequest(String comment, Map<String, Serializable> variables, boolean badJsonFormat) {
        super();
        this.comment = comment;
        this.variables = variables;
        this.badJsonFormat = badJsonFormat;
    }

    public String getComment() {
        if (StringUtils.isNotBlank(comment)) {
            return comment;
        }

        // otherwise get the comment from the variables, if any,
        // so that it will be logged by the audit
        return (String) variables.get(GraphNode.NODE_VARIABLE_COMMENT);
    }

    public Map<String, Object> getDataMap() {
        Map<String, Object> data = new HashMap<String, Object>();
        if (getVariables() != null) {
            data.put(Constants.VAR_WORKFLOW, getVariables());
            data.put(Constants.VAR_WORKFLOW_NODE, getVariables());
        }
        if (badJsonFormat) {
            data.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, badJsonFormat);
        }
        if (StringUtils.isNotBlank(getComment())) {
            data.put(GraphNode.NODE_VARIABLE_COMMENT, getComment());
        }
        return data;
    }

    public Map<String, Serializable> getVariables() {
        return variables;
    }

}
