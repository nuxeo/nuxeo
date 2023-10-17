/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.core.bulk;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;

/**
 * @since 2021.45
 */
public class TestBulkStatus {

    protected final String COUNT_VAR_NAME = "var";

    @Test
    public void testMergeResultNumberOverflow() {
        // 2147483647 (max int) + 1 = 2147483648
        BulkStatus status = new BulkStatus("commandId");
        Map<String, Serializable> map = new HashMap<>();
        map.put(COUNT_VAR_NAME, 2147483647);
        status.setResult(map);
        Map<String, Serializable> otherMap = new HashMap<>();
        otherMap.put(COUNT_VAR_NAME, 1);
        status.mergeResult(otherMap);
        var actual = status.getResult().get(COUNT_VAR_NAME);
        assertEquals(2147483648L, actual);

        // 4,000,000,000 + 42 = 4,000,000,042
        status = new BulkStatus("commandId");
        map = new HashMap<>();
        map.put(COUNT_VAR_NAME, 4000000000L);
        status.setResult(map);
        otherMap = new HashMap<>();
        otherMap.put(COUNT_VAR_NAME, 42);
        status.mergeResult(otherMap);
        actual = status.getResult().get(COUNT_VAR_NAME);
        assertEquals(4000000042L, actual);

        // 3.4e8f + 3 = 34,0000,003
        status = new BulkStatus("commandId");
        map = new HashMap<>();
        map.put(COUNT_VAR_NAME, 3.4e8f);
        status.setResult(map);
        otherMap = new HashMap<>();
        otherMap.put(COUNT_VAR_NAME, 3);
        status.mergeResult(otherMap);
        actual = status.getResult().get(COUNT_VAR_NAME);
        assertEquals(340000003D, actual);
    }

    @Test
    public void testMergeResultNullValue() {
        BulkStatus status = new BulkStatus("commandId");
        Map<String, Serializable> map = new HashMap<>();
        map.put(COUNT_VAR_NAME, null);
        status.setResult(map);
        Map<String, Serializable> otherMap = new HashMap<>();
        otherMap.put(COUNT_VAR_NAME, 1);
        status.mergeResult(otherMap);
        var actual = status.getResult().get(COUNT_VAR_NAME);
        assertEquals(1, actual);

        status = new BulkStatus("commandId");
        map = new HashMap<>();
        map.put(COUNT_VAR_NAME, 1);
        status.setResult(map);
        otherMap = new HashMap<>();
        otherMap.put(COUNT_VAR_NAME, null);
        status.mergeResult(otherMap);
        actual = status.getResult().get(COUNT_VAR_NAME);
        assertEquals(1, actual);
    }

}
