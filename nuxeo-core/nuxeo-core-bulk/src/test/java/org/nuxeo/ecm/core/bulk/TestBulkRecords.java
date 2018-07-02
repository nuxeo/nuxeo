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
package org.nuxeo.ecm.core.bulk;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.nuxeo.lib.stream.computation.Record;

/**
 * The aim of this test class is to enforce backward compatibility of {@link Record} sent in bulk action streams.
 *
 * @since 10.2
 */
public class TestBulkRecords {

    @Test
    public void testRecordCreation() {
        Record record = BulkRecords.of("bulkId", 10L, asList("id1", "id2", "id3"));
        String key = record.getKey();
        String value = new String(record.getData(), UTF_8);

        assertEquals("bulkId:10", key);
        assertEquals(value, "id1_id2_id3");
    }

    @Test
    public void testInitialFormat() {
        Record record = Record.of("bulkId:12345", "id1_id2_id3".getBytes(UTF_8));

        String bulkId = BulkRecords.bulkIdFrom(record);
        assertEquals("bulkId", bulkId);

        List<String> docIds = BulkRecords.docIdsFrom(record);
        assertEquals(3, docIds.size());
        assertEquals(asList("id1", "id2", "id3"), docIds);
    }

}
