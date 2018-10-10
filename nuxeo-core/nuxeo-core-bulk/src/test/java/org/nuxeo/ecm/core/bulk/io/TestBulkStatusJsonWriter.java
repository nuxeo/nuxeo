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
package org.nuxeo.ecm.core.bulk.io;

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
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.time.Instant;
import java.util.Collections;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.2
 */
@Features(CoreBulkFeature.class)
public class TestBulkStatusJsonWriter extends AbstractJsonWriterTest.Local<BulkStatusJsonWriter, BulkStatus> {

    public TestBulkStatusJsonWriter() {
        super(BulkStatusJsonWriter.class, BulkStatus.class);
    }

    @Test
    public void testDefault() throws Exception {
        String zeroId = "00000000-0000-0000-0000-000000000000";
        Instant instant = Instant.now();

        BulkStatus status = new BulkStatus(zeroId);
        status.setState(State.COMPLETED);
        status.setSubmitTime(instant);
        status.setScrollStartTime(instant.plusSeconds(1));
        status.setScrollEndTime(instant.plusSeconds(2));
        status.setCompletedTime(instant.plusSeconds(3));
        status.setResult(Collections.singletonMap("result", "test"));

        JsonAssert json = jsonAssert(status);
        json.has(ENTITY_FIELD_NAME).isEquals(STATUS_ENTITY_TYPE);
        json.has(STATUS_COMMAND_ID).isEquals(status.getCommandId());
        json.has(STATUS_STATE).isEquals(status.getState().toString());
        json.has(STATUS_SUBMIT_TIME).isEquals(status.getSubmitTime().toString());
        json.has(STATUS_SCROLL_START).isEquals(status.getScrollStartTime().toString());
        json.has(STATUS_SCROLL_END).isEquals(status.getScrollEndTime().toString());
        json.has(STATUS_COMPLETED_TIME).isEquals(status.getCompletedTime().toString());
        json.has(STATUS_TOTAL).isEquals(0);
        json.has(STATUS_PROCESSED).isEquals(0);
        json.has(STATUS_RESULT).has("result").isEquals("test");
    }
}
