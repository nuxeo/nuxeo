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

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestBulkCommandAvro {

    @Rule
    public final CodecTestRule<BulkCommand> codecRule = new CodecTestRule<>("avro", BulkCommand.class);

    @Test
    public void testCommandWithoutParameters() {
        BulkCommand command = new BulkCommand().withUsername("username")
                                               .withRepository("default")
                                               .withQuery("SELECT * FROM Document")
                                               .withAction("action");
        BulkCommand actualCommand = codecRule.encodeDecode(command);

        assertEquals(command, actualCommand);
    }

    @Test
    public void testCommandWithSimpleParameters() {
        BulkCommand command = new BulkCommand().withUsername("username")
                                               .withRepository("default")
                                               .withQuery("SELECT * FROM Document")
                                               .withAction("action")
                                               .withParam("key1", "value1")
                                               .withParam("key2", "value2");
        BulkCommand actualCommand = codecRule.encodeDecode(command);

        assertEquals(command, actualCommand);
    }

}
