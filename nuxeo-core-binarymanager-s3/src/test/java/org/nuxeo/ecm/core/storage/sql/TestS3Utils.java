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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.S3Utils.SliceConsumer;

public class TestS3Utils {

    @Test
    public void testProcessSlices() {
        List<String> list = new ArrayList<>();
        SliceConsumer recorder = (num, begin, end) -> {
            list.add(num + ":" + begin + "-" + end);
        };

        // typical case
        list.clear();
        S3Utils.processSlices(10, 25, recorder);
        assertEquals(Arrays.asList("0:0-10", "1:10-20", "2:20-25"), list);

        // exactly at the end
        list.clear();
        S3Utils.processSlices(10, 30, recorder);
        assertEquals(Arrays.asList("0:0-10", "1:10-20", "2:20-30"), list);

        // exactly one slice
        list.clear();
        S3Utils.processSlices(10, 10, recorder);
        assertEquals(Arrays.asList("0:0-10"), list);

        // slice smaller than total length
        list.clear();
        S3Utils.processSlices(10, 5, recorder);
        assertEquals(Arrays.asList("0:0-5"), list);

        // degenerate case
        list.clear();
        S3Utils.processSlices(10, 0, recorder);
        assertEquals(Collections.emptyList(), list);
    }

}
