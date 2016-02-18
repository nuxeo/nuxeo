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

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.DERIVATIVE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.SessionWrapper;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.webengine.WebException;

/**
 *
 * @since 8.2
 * @deprecated use @{link TaskCompletionRequestJsonReader TaskCompletionRequestJsonReader}
 */
@Setup(mode = SINGLETON, priority = DERIVATIVE)
@Deprecated
public class TaskCompletionRequestLegacyJsonReader extends EntityJsonReader<TaskCompletionRequest> {

    public TaskCompletionRequestLegacyJsonReader() {
        super(TaskCompletionRequestJsonReader.ENTITY_TYPE);
    }

    @Override
    protected TaskCompletionRequest readEntity(JsonNode jn) throws IOException {
        String id = jn.get("id").getValueAsText();
        String comment = jn.get("comment").getValueAsText();
        JsonNode variableNode = jn.get("variables");
        Map<String, Serializable> variables = null;

        if (id == null) {
            throw new WebException("No id found in request body", Response.Status.BAD_REQUEST.getStatusCode());
        }

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

        return new TaskCompletionRequest(comment, variables, true);
    }

}
