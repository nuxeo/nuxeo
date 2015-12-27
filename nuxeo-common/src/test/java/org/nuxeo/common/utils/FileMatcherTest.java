/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.nuxeo.common.utils.FileMatcher;

/**
 *
 */
public class FileMatcherTest {

    @Test
    public void test() {
        FileMatcher fm = FileMatcher.getMatcher("nuxeo-automation-1.2.jar");
        assertTrue(fm.match("nuxeo-automation-1.2.jar"));
        assertNull(fm.getKey());
        assertNull(fm.getValue());

        fm = FileMatcher.getMatcher("nuxeo-automation-{v:.+}.jar");
        assertTrue(fm.match("nuxeo-automation-1.2.jar"));
        assertEquals("Key not extracted", "v", fm.getKey());
        assertEquals("Value not extracted", "1.2", fm.getValue());

        fm = FileMatcher.getMatcher("nuxeo-automation-{v:.+}");
        assertTrue(fm.match("nuxeo-automation-1.2.jar"));
        assertEquals("Value not extracted", "1.2.jar", fm.getValue());

        fm = FileMatcher.getMatcher("{v:.+}.jar");
        assertTrue(fm.match("nuxeo-automation-1.2.jar"));
        assertEquals("Value not extracted", "nuxeo-automation-1.2", fm.getValue());

        fm = FileMatcher.getMatcher("{v:.+}");
        assertTrue(fm.match("nuxeo-automation-1.2.jar"));
        assertEquals("Value not extracted", "nuxeo-automation-1.2.jar", fm.getValue());

        fm = FileMatcher.getMatcher("{n:.*-}[0-9]+.*\\.jar");
        assertTrue(fm.match("nuxeo-automation-5.5-SNAPSHOT.jar"));
        assertEquals("Value not extracted", "nuxeo-automation-", fm.getValue());
        assertTrue(fm.match("nuxeo-automation-5.5.jar"));
        assertEquals("Value not extracted", "nuxeo-automation-", fm.getValue());

        fm = FileMatcher.getMatcher(fm.getValue() + "{v:[0-9]+.*}\\.jar");
        assertTrue(fm.match("nuxeo-automation-5.5-SNAPSHOT.jar"));
        assertEquals("Value not extracted", "5.5-SNAPSHOT", fm.getValue());
        assertTrue(fm.match("nuxeo-automation-5.5.jar"));
        assertEquals("Value not extracted", "5.5", fm.getValue());
    }

}
