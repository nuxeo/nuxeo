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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.automation.io.services.bulk;

import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_ENTITY_TYPE;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_OPERATION;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_QUERY;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_REPOSITORY;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.COMMAND_USERNAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.util.function.Function;

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
        // everything is mandatory
        Function<String, String> getter = fieldName -> jn.get(fieldName).asText();
        return new BulkCommand().withUsername(getter.apply(COMMAND_USERNAME))
                                .withRepository(getter.apply(COMMAND_REPOSITORY))
                                .withQuery(getter.apply(COMMAND_QUERY))
                                .withAction(getter.apply(COMMAND_OPERATION));
    }
}
