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
        return comment;
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
