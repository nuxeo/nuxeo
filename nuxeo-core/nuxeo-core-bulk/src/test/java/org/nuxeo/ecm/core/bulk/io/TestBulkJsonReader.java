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

import java.io.File;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.message.BulkStatus.State;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReaderTest;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.2
 */
@Features(CoreBulkFeature.class)
public class TestBulkJsonReader extends AbstractJsonReaderTest.Local<BulkJsonReader, BulkStatus> {

    public TestBulkJsonReader() {
        super(BulkJsonReader.class, BulkStatus.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefault() throws Exception {
        File file = FileUtils.getResourceFileFromContext("bulk-status-test-default.json");
        BulkStatus status = asObject(file);
        assertEquals("00000000-0000-0000-0000-000000000000", status.getCommandId());
        assertEquals(State.SCHEDULED, status.getState());
        assertEquals(Instant.parse("2018-06-21T12:37:08.172Z"), status.getSubmitTime());
        Map<String, Serializable> result = status.getResult();
        assertEquals("o1", result.get("result1"));
        assertEquals("o2", ((List<String>) result.get("result2")).get(0));
        assertEquals("o3", ((List<String>) result.get("result2")).get(1));
    }
}
