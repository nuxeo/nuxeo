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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReaderTest;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.2
 */
@Features(CoreBulkFeature.class)
public class TestBulkCommandJsonReader extends AbstractJsonReaderTest.Local<BulkCommandJsonReader, BulkCommand> {

    public TestBulkCommandJsonReader() {
        super(BulkCommandJsonReader.class, BulkCommand.class);
    }

    @Test
    public void testDefault() throws Exception {
        File file = FileUtils.getResourceFileFromContext("bulk-command-test-default.json");
        BulkCommand command = asObject(file);
        assertEquals("myUser", command.getUsername());
        assertEquals("myRepository", command.getRepository());
        assertEquals("SELECT * FROM Document", command.getQuery());
        assertEquals("myAction", command.getAction());

        assertEquals(4, command.getParams().size());
        assertEquals("mySpecificParameter", command.getParam("actionParam"));
        assertTrue(command.getParam("boolean"));
        assertEquals(1200, command.<Long>getParam("long").longValue());
        Map<String, Serializable> complex = command.getParam("complex");
        assertFalse(complex.isEmpty());
        assertEquals("value", complex.get("key"));
    }
}
