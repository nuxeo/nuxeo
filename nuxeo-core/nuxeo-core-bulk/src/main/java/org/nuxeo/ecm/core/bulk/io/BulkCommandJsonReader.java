/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */
package org.nuxeo.ecm.core.bulk.io;

import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_ACTION;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_ENTITY_TYPE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_PARAMS;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_QUERY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_REPOSITORY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_USERNAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BulkCommandJsonReader extends EntityJsonReader<BulkCommand> {

    public BulkCommandJsonReader() {
        super(COMMAND_ENTITY_TYPE);
    }

    @Override
    protected BulkCommand readEntity(JsonNode jn) {
        // everything is mandatory except parameters
        Function<String, String> getter = fieldName -> jn.get(fieldName).asText();

        Map<String, String> params = new HashMap<>();
        JsonNode commandNode = jn.get(COMMAND_PARAMS);
        if (commandNode != null) {
            fillParams(jn.get(COMMAND_PARAMS), params);
        }

        return new BulkCommand().withUsername(getter.apply(COMMAND_USERNAME))
                                .withRepository(getter.apply(COMMAND_REPOSITORY))
                                .withQuery(getter.apply(COMMAND_QUERY))
                                .withAction(getter.apply(COMMAND_ACTION))
                                .withParams(params);
    }

    protected void fillParams(JsonNode node, Map<String, String> params) {
        for (Iterator<Map.Entry<String, JsonNode>> paramsNode = node.fields(); paramsNode.hasNext();) {
            Map.Entry<String, JsonNode> paramNode = paramsNode.next();
            switch (paramNode.getValue().getNodeType()) {
            case STRING:
            case BOOLEAN:
            case NUMBER:
            case BINARY: // binary will be converted to base64
                params.put(paramNode.getKey(), paramNode.getValue().asText());
                break;
            case ARRAY:
            case OBJECT:
            default:
                throw new NuxeoException("Node type=" + paramNode.getValue().getNodeType() + " is not supported");
            }
        }
    }

}
