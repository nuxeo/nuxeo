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
 *     pierre
 */
package org.nuxeo.ecm.automation.io.services.bulk;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.bulk.BulkStatus;
import org.nuxeo.ecm.core.bulk.BulkStatus.State;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BulkJsonReader extends EntityJsonReader<BulkStatus> {

    public BulkJsonReader() {
        super("bulk");
    }

    @Override
    public BulkStatus readEntity(JsonNode jn) throws IOException {
        // misc
        String creation = jn.get("creation").asText();
        State state = State.valueOf(jn.get("state").asText());
        // command
        String query = jn.get("query").asText();
        String operation = jn.get("operation").asText();
        String repository = jn.get("repository").asText();
        String username = jn.get("username").asText();
        Long scrolledDocumentCount = null;
        if (jn.get("scrolledDocumentCount").isNumber()) {
            scrolledDocumentCount = Long.valueOf(jn.get("scrolledDocumentCount").longValue());
        }
        BulkCommand command = new BulkCommand().withOperation(operation)
                                               .withQuery(query)
                                               .withRepository(repository)
                                               .withUsername(username);
        BulkStatus status = new BulkStatus();
        status.setCommand(command);
        status.setState(state);
        status.setCreationDate(ZonedDateTime.parse(creation));
        status.setScrolledDocumentCount(scrolledDocumentCount);
        return status;
    }

}
