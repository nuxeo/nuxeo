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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.BULK_COMMAND;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.BULK_COUNT;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.BULK_SUBMIT;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.BULK_ENTITY_TYPE;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.BULK_ID;
import static org.nuxeo.ecm.automation.io.services.bulk.BulkConstants.BULK_STATE;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.time.Instant;

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
        super(BULK_ENTITY_TYPE);
    }

    @Override
    public BulkStatus readEntity(JsonNode jn) throws IOException {
        BulkStatus status = new BulkStatus();

        String id = jn.get(BULK_ID).asText();
        status.setId(id);

        String state = getStringField(jn, BULK_STATE);
        if (isNotEmpty(state)) {
            status.setState(State.valueOf(state));
        }

        String creation = getStringField(jn, BULK_SUBMIT);

        if (isNotEmpty(creation)) {
            status.setSubmitTime(Instant.parse(creation));
        }

        JsonNode jnCommand = jn.get(BULK_COMMAND);
        if (jnCommand != null && !jnCommand.isNull()) {
            BulkCommand command = readEntity(BulkCommand.class, BulkCommand.class, jnCommand);
            status.setCommand(command);
        }

        Long count = getLongField(jn, BULK_COUNT);
        status.setCount(count);

        return status;
    }

}
