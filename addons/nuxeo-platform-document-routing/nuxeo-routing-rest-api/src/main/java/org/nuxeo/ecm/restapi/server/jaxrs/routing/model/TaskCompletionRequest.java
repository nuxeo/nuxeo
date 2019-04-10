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

package org.nuxeo.ecm.restapi.server.jaxrs.routing.model;

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

    protected String comment;

    protected Map<String, Serializable> variables;

    public TaskCompletionRequest() {
        super();
    }

    public String getComment() {
        return comment;
    }

    public Map<String, Object> getDataMap() {
        Map<String, Object> data = new HashMap<String, Object>();
        if (getVariables() != null) {
            data.put(Constants.VAR_WORKFLOW, getVariables());
            data.put(Constants.VAR_WORKFLOW_NODE, getVariables());
        }
        data.put(DocumentRoutingConstants._MAP_VAR_FORMAT_JSON, Boolean.TRUE);
        if (StringUtils.isNotBlank(getComment())) {
            data.put(GraphNode.NODE_VARIABLE_COMMENT, getComment());
        }
        return data;
    }

    public Map<String, Serializable> getVariables() {
        return variables;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setVariables(Map<String, Serializable> nodeVariables) {
        this.variables = nodeVariables;
    }

}
