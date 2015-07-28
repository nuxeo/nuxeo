/*
 * Copyright (c) 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        ;

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
