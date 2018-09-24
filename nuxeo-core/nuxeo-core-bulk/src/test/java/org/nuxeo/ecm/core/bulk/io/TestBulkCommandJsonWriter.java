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

import static java.util.Collections.singletonMap;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_ACTION;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_ENTITY_TYPE;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_PARAMS;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_QUERY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_REPOSITORY;
import static org.nuxeo.ecm.core.bulk.io.BulkConstants.COMMAND_USERNAME;
import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;

import java.util.HashMap;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonWriterTest;
import org.nuxeo.ecm.core.io.marshallers.json.JsonAssert;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.2
 */
@Features(CoreBulkFeature.class)
public class TestBulkCommandJsonWriter extends AbstractJsonWriterTest.Local<BulkCommandJsonWriter, BulkCommand> {

    public TestBulkCommandJsonWriter() {
        super(BulkCommandJsonWriter.class, BulkCommand.class);
    }

    @Test
    public void testDefault() throws Exception {
        BulkCommand command = new BulkCommand().withUsername("myUser")
                                               .withRepository("myRepository")
                                               .withQuery("SELECT * FROM Document")
                                               .withAction("myAction")
                                               .withParam("actionParam", "mySpecificParameter")
                                               .withParam("boolean", false)
                                               .withParam("long", 1200)
                                               .withParam("complex", new HashMap<>(singletonMap("key", "value")));
        JsonAssert json = jsonAssert(command);
        json.properties(6);
        json.has(ENTITY_FIELD_NAME).isEquals(COMMAND_ENTITY_TYPE);
        json.has(COMMAND_USERNAME).isEquals("myUser");
        json.has(COMMAND_REPOSITORY).isEquals("myRepository");
        json.has(COMMAND_QUERY).isEquals("SELECT * FROM Document");
        json.has(COMMAND_ACTION).isEquals("myAction");
        JsonAssert params = json.has(COMMAND_PARAMS).properties(4);
        params.has("actionParam").isEquals("mySpecificParameter");
        params.has("boolean").isEquals(false);
        params.has("long").isEquals(1200);
        JsonAssert complex = params.has("complex").properties(1);
        complex.has("key").isEquals("value");
    }
}
