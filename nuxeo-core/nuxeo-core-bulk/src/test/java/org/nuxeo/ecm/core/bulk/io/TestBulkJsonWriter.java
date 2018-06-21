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

import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_COMMAND;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_ENTITY_TYPE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_ID;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_STATE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.BULK_SUBMIT;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_ACTION;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_PARAMS;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_QUERY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_REPOSITORY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_USERNAME;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.time.Instant;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.BulkCommand;
import org.nuxeo.ecm.core.bulk.BulkStatus;
import org.nuxeo.ecm.core.bulk.BulkStatus.State;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.2
 */
@Features(CoreBulkFeature.class)
public class TestBulkJsonWriter extends AbstractJsonWriterTest.Local<BulkJsonWriter, BulkStatus> {

    public TestBulkJsonWriter() {
        super(BulkJsonWriter.class, BulkStatus.class);
    }

    @Test
    public void testDefault() throws Exception {
        String zeroId = "00000000-0000-0000-0000-000000000000";
        String instant = "2018-06-21T12:37:08.172Z";

        BulkStatus status = new BulkStatus();
        status.setId(zeroId);
        status.setCommand(new BulkCommand().withUsername("myUser")
                                           .withRepository("myRepository")
                                           .withQuery("SELECT * FROM Document")
                                           .withAction("myAction"));
        status.setState(State.SCHEDULED);
        status.setSubmitTime(Instant.parse(instant));

        JsonAssert json = jsonAssert(status);
        json.properties(5);
        json.has(ENTITY_FIELD_NAME).isEquals(BULK_ENTITY_TYPE);
        json.has(BULK_ID).isEquals(status.getId());
        json.has(BULK_STATE).isEquals(status.getState().toString());
        json.has(BULK_SUBMIT).isEquals(instant);

        JsonAssert command = json.has(BULK_COMMAND).properties(5);
        command.has(COMMAND_USERNAME).isEquals("myUser");
        command.has(COMMAND_REPOSITORY).isEquals("myRepository");
        command.has(COMMAND_QUERY).isEquals("SELECT * FROM Document");
        command.has(COMMAND_ACTION).isEquals("myAction");
        command.hasNot(COMMAND_PARAMS);
    }
}
