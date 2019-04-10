/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.routing.core.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.DERIVATIVE;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

/**
 * @since 8.2
 * @deprecated use {@link WorkflowRequestJsonReader WorkflowRequestJsonReader}
 */
@Setup(mode = SINGLETON, priority = DERIVATIVE)
@Deprecated
public class WorkflowRequestLegacyJsonReader extends EntityJsonReader<WorkflowRequest> {

    protected static final Log log = LogFactory.getLog(WorkflowRequestLegacyJsonReader.class);

    public static final String ENTITY_TYPE = "workflow";

    public WorkflowRequestLegacyJsonReader() {
        super(WorkflowRequestLegacyJsonReader.ENTITY_TYPE);
    }

    @Override
    protected WorkflowRequest readEntity(JsonNode jn) throws IOException {
        String workflowModelName = getStringField(jn, "workflowModelName");
        List<String> attachedDocumentIds = new ArrayList<String>();
        JsonNode attachedDocumentIdsNode = jn.get("attachedDocumentIds");
        if (attachedDocumentIdsNode != null) {
            for (Iterator<JsonNode> it = attachedDocumentIdsNode.getElements(); it.hasNext();) {
                attachedDocumentIds.add(it.next().getTextValue());
            }
        }
        JsonNode variableNode = jn.get("variables");
        Map<String, Serializable> variables = null;

        try (SessionWrapper closeable = ctx.getSession(null)) {
            CoreSession session = closeable.getSession();
            if (variableNode != null) {
                try {
                    variables = JsonEncodeDecodeUtils.decodeVariables(variableNode,
                            null, session);
                } catch (ClassNotFoundException e) {
                    throw new MarshallingException(e);
                }
            }
        }

        return new WorkflowRequest(workflowModelName, attachedDocumentIds, variables);
    }

}
