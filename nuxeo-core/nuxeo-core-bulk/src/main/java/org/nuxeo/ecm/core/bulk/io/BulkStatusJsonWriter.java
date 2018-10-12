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
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 10.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class BulkStatusJsonWriter extends ExtensibleEntityJsonWriter<BulkStatus> {

    public BulkStatusJsonWriter() {
        super(STATUS_ENTITY_TYPE, BulkStatus.class);
    }

    @Override
    public void writeEntityBody(BulkStatus entity, JsonGenerator jg) throws IOException {
        jg.writeStringField(STATUS_COMMAND_ID, entity.getCommandId());
        jg.writeStringField(STATUS_STATE, entity.getState() != null ? entity.getState().toString() : null);
        jg.writeNumberField(STATUS_PROCESSED, entity.getProcessed());
        jg.writeNumberField(STATUS_TOTAL, entity.getTotal());
        jg.writeStringField(STATUS_ACTION, entity.getAction());
        jg.writeStringField(STATUS_SUBMIT_TIME,
                entity.getSubmitTime() != null ? entity.getSubmitTime().toString() : null);
        jg.writeStringField(STATUS_SCROLL_START,
                entity.getScrollStartTime() != null ? entity.getScrollStartTime().toString() : null);
        jg.writeStringField(STATUS_SCROLL_END,
                entity.getScrollEndTime() != null ? entity.getScrollEndTime().toString() : null);
        jg.writeStringField(STATUS_COMPLETED_TIME,
                entity.getCompletedTime() != null ? entity.getCompletedTime().toString() : null);
        Map<String, Serializable> result = entity.getResult();
        if (!result.isEmpty()) {
            jg.writeObjectField(STATUS_RESULT, result);
        }
    }

}
