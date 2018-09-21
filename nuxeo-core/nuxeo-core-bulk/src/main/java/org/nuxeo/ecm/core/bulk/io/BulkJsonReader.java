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

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_COUNT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_ENTITY_TYPE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_ID;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_RESULT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_STATE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_SUBMIT;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import org.nuxeo.ecm.core.bulk.io.BulkParameters;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
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
    public BulkStatus readEntity(JsonNode jn) {
        BulkStatus status = new BulkStatus();

        String id = jn.get(BULK_ID).asText();
        status.setCommandId(id);

        String state = getStringField(jn, BULK_STATE);
        if (isNotEmpty(state)) {
            status.setState(State.valueOf(state));
        }

        String creation = getStringField(jn, BULK_SUBMIT);

        if (isNotEmpty(creation)) {
            status.setSubmitTime(Instant.parse(creation));
        }

        Long count = getLongField(jn, BULK_COUNT);
        if (count != null) {
            status.setCount(count);
        }

        Long processed = getLongField(jn, BULK_PROCESSED);
        if (processed != null) {
            status.setProcessed(processed);
        }

        Map<String, Serializable> result = emptyMap();
        if (jn.has(BULK_RESULT)) {
            result = BulkParameters.paramsToMap(jn.get(BULK_RESULT));
        }
        status.setResult(result);

        return status;
    }

}
