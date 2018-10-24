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
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ACTION;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_COMMAND_ID;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_COMPLETED_TIME;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_ENTITY_TYPE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_PROCESSED;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_RESULT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SCROLL_END;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SCROLL_START;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_STATE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_SUBMIT_TIME;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_TOTAL;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.STATUS_USERNAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BulkStatusJsonReader extends EntityJsonReader<BulkStatus> {

    public BulkStatusJsonReader() {
        super(STATUS_ENTITY_TYPE);
    }

    @Override
    public BulkStatus readEntity(JsonNode jn) {
        String id = jn.get(STATUS_COMMAND_ID).asText();
        BulkStatus status = new BulkStatus(id);

        String state = getStringField(jn, STATUS_STATE);
        if (isNotEmpty(state)) {
            status.setState(State.valueOf(state));
        }
        String action = getStringField(jn, STATUS_ACTION);
        if (isNotEmpty(action)) {
            status.setAction(action);
        }

        Long processed = getLongField(jn, STATUS_PROCESSED);
        if (processed != null) {
            status.setProcessed(processed);
        }
        Long count = getLongField(jn, STATUS_TOTAL);
        if (count != null) {
            status.setTotal(count);
        }

        String instantString = getStringField(jn, STATUS_SUBMIT_TIME);
        if (isNotEmpty(instantString)) {
            status.setSubmitTime(Instant.parse(instantString));
        }
        instantString = getStringField(jn, STATUS_SCROLL_START);
        if (isNotEmpty(instantString)) {
            status.setScrollStartTime(Instant.parse(instantString));
        }
        instantString = getStringField(jn, STATUS_SCROLL_END);
        if (isNotEmpty(instantString)) {
            status.setScrollEndTime(Instant.parse(instantString));
        }
        instantString = getStringField(jn, STATUS_COMPLETED_TIME);
        if (isNotEmpty(instantString)) {
            status.setCompletedTime(Instant.parse(instantString));
        }
        instantString = getStringField(jn, STATUS_USERNAME);
        if (isNotEmpty(instantString)) {
            status.setUsername(instantString);
        }
        Map<String, Serializable> result = emptyMap();
        if (jn.has(STATUS_RESULT)) {
            result = BulkParameters.paramsToMap(jn.get(STATUS_RESULT));
        }
        status.setResult(result);

        return status;
    }

}
