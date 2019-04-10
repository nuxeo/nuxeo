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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @since 7.2
 */
public class WorkflowRequest {

    protected List<String> attachedDocumentIds = new ArrayList<String>();

    protected Map<String, Serializable> variables;

    protected String workflowModelName;

    public WorkflowRequest() {
        super();
    }

    /**
     * @since 8.2
     */
    public WorkflowRequest(String workflowModelName, List<String> attachedDocumentIds,
            Map<String, Serializable> variables) {
        super();
        this.attachedDocumentIds = attachedDocumentIds;
        this.variables = variables;
        this.workflowModelName = workflowModelName;
    }

    public List<String> getAttachedDocumentIds() {
        return attachedDocumentIds;
    }

    public String getWorkflowModelName() {
        return workflowModelName;
    }

    public Map<String, Serializable> getVariables() {
        return variables;
    }

    public void setAttachedDocumentIds(List<String> attachedDocumentIds) {
        this.attachedDocumentIds = attachedDocumentIds;
    }

    public void setWorkflowModelName(String workflowModelName) {
        this.workflowModelName = workflowModelName;
    }

    public void setVariables(Map<String, Serializable> worflowVariables) {
        this.variables = worflowVariables;
    }

}
