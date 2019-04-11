/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.model;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.nuxeo.ecm.core.utils.SIDGenerator;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule.IgnoreWindows;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ConditionalIgnoreRule.Feature.class)
public class TestSidGenerator {

    @Test
    public void testGenerator() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            long id = SIDGenerator.next();
            if (!ids.add(id)) {
                fail("ID already generated: " + id);
            }
        }
    }

    @Test
    @ConditionalIgnoreRule.Ignore(condition = IgnoreWindows.class, cause = "windows doesn't have enough time granularity for such a high-speed test")
    public void testGeneratorReset() throws Exception {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            long id = SIDGenerator.next();
            if (!ids.add(id)) {
                fail("ID already generated: " + id);
            }
        }

        // change the counter to a value near the max one to force a counter reset
        Field field = SIDGenerator.class.getDeclaredField("count");
        field.setAccessible(true);
        field.set(null, Integer.MAX_VALUE - 1000);
        Thread.sleep(1);

        for (int i = 0; i < 3000; i++) {
            long id = SIDGenerator.next();
            if (!ids.add(id)) {
                fail("ID already generated: " + id);
            }
        }

        Integer counter = (Integer) field.get(null);
        assertEquals(2000, counter.intValue());
    }

}
