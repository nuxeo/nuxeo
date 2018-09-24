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

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonBijectionTest;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreBulkFeature.class)
public class TestBulkCommandJson extends AbstractJsonBijectionTest<BulkCommand> {

    public TestBulkCommandJson() {
        super(BulkCommand.class);
    }

    @Test
    public void testFileToObjectToFile() throws Exception {
        File file = FileUtils.getResourceFileFromContext("bulk-command-test-default.json");

        BulkCommand object = asObject(file);
        String json = asJson(object);

        assertContentEquals(file, json);
    }

}
