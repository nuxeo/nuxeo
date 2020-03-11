/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tiry
 */

package org.nuxeo.elasticsearch.test;

import org.junit.Assert;
import org.junit.Test;
import org.nuxeo.elasticsearch.Timestamp;

public class TestTimestamp {

    @Test
    public void testTimestamp() throws Exception {
        long t0 = Timestamp.currentTimeMicros();
        Assert.assertTrue(t0 > 0);

        long t1 = Timestamp.currentTimeMicros();
        Assert.assertTrue(t1 > t0);

        int delay = 5;
        Thread.sleep(delay);

        long t2 = Timestamp.currentTimeMicros();
        Assert.assertTrue((t2 - t1) >= (delay * 1000));
    }

}
