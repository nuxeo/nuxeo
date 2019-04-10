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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.core.impl.GraphNode;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;

/**
 * @since 7.2
 */
public class NodeAccessRunner extends UnrestrictedSessionRunner {

    GraphNode node;

    GraphRoute workflowInstance;

    String workflowInstanceId;

    String nodeId;

    public NodeAccessRunner(CoreSession session, String workflowInstanceId, String nodeId) {
        super(session);
        this.workflowInstanceId = workflowInstanceId;
        this.nodeId = nodeId;
    }

    /**
     * @since 8.2
     */
    public GraphNode getNode() {
        return node;
    }

    /**
     * @since 8.2
     */
    public GraphRoute getWorkflowInstance() {
        return workflowInstance;
    }

    @Override
    public void run() {
        DocumentModel workflowInstanceDoc = session.getDocument(new IdRef(workflowInstanceId));
        workflowInstance = workflowInstanceDoc.getAdapter(GraphRoute.class);
        node = workflowInstance.getNode(nodeId);
        workflowInstanceDoc.detach(true);
        node.getDocument().detach(true);
    }

}
