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

import java.util.Map;

/**
 * @since 7.1
 */
public class TaskCompletionRequest {

    public TaskCompletionRequest() {
        super();
    }

    protected String comment;

    protected Map<String, String> nodeVariables;

    protected Map<String, String> workflowVariables;

    public String getComment() {
        return comment;
    }

    public Map<String, String> getNodeVariables() {
        return nodeVariables;
    }

    public Map<String, String> getWorkflowVariables() {
        return workflowVariables;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setNodeVariables(Map<String, String> nodeVariables) {
        this.nodeVariables = nodeVariables;
    }

    public void setWorkflowVariables(Map<String, String> workflowVariables) {
        this.workflowVariables = workflowVariables;
    }

}
