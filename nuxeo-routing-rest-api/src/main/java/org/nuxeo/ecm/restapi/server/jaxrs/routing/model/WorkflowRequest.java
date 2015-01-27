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
