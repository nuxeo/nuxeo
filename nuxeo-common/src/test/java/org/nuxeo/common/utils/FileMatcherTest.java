/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
        assertEquals("Value not extracted", "nuxeo-automation-1.2",
                fm.getValue());

        fm = FileMatcher.getMatcher("{v:.+}");
        assertTrue(fm.match("nuxeo-automation-1.2.jar"));
        assertEquals("Value not extracted", "nuxeo-automation-1.2.jar",
                fm.getValue());

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
