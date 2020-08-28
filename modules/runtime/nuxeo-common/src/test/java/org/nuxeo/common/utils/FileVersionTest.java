/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FileVersionTest {

    @Test
    public void test() {
        // test version containing only major part
        assertArrayEquals(new FileVersion("1").getSplitVersion(), new Integer[] { 1 });
        FileVersion fv = new FileVersion("1-qualifier");
        assertArrayEquals(fv.getSplitVersion(), new Integer[] { 1 });
        assertEquals("-qualifier", fv.getQualifier());
        fv = new FileVersion("1_qualifier");
        assertArrayEquals(fv.getSplitVersion(), new Integer[] { 1 });
        assertEquals("_qualifier", fv.getQualifier());

        // test caja versions
        fv = new FileVersion("r1234");
        assertArrayEquals(fv.getSplitVersion(), new Integer[] { 1234 });
        fv = new FileVersion("r1234-qualifier");
        assertArrayEquals(fv.getSplitVersion(), new Integer[] { 1234 });
        assertEquals("-qualifier", fv.getQualifier());
        fv = new FileVersion("r1234_qualifier");
        assertArrayEquals(fv.getSplitVersion(), new Integer[] { 1234 });
        assertEquals("_qualifier", fv.getQualifier());

        assertArrayEquals(new FileVersion("1.3.3").getSplitVersion(), new Integer[] { 1, 3, 3 });
        assertEquals(new FileVersion("1.3.3").getQualifier(), "");
        assertArrayEquals(new FileVersion("1.3.3_01").getSplitVersion(), new Integer[] { 1, 3, 3 });
        assertEquals(new FileVersion("1.3.3_01").getQualifier(), "_01");
        assertArrayEquals(new FileVersion("1.1-BETA5-incubating").getSplitVersion(), new Integer[] { 1, 1 });
        assertEquals(new FileVersion("1.1-BETA5-incubating").getQualifier(), "-BETA5-incubating");
        assertArrayEquals(new FileVersion("3.3.1.GA-NX").getSplitVersion(), new Integer[] { 3, 3, 1 });
        assertEquals(new FileVersion("3.3.1.GA-NX").getQualifier(), ".GA-NX");
        assertArrayEquals(new FileVersion("1.3.1-NXP-7750").getSplitVersion(), new Integer[] { 1, 3, 1 });
        assertEquals(new FileVersion("1.3.1-NXP-7750").getQualifier(), "-NXP-7750");
        assertArrayEquals(new FileVersion("3.0-NX3.2").getSplitVersion(), new Integer[] { 3, 0 });
        assertEquals(new FileVersion("3.0-NX3.2").getQualifier(), "-NX3.2");
        assertArrayEquals(new FileVersion("0.4.0-r1096750").getSplitVersion(), new Integer[] { 0, 4, 0 });
        assertEquals(new FileVersion("0.4.0-r1096750").getQualifier(), "-r1096750");

        assertTrue("5.5 must be greater than 5.4", new FileVersion("5.5").compareTo(new FileVersion("5.4")) > 0);
        assertTrue("5.5-SNAPSHOT must be greater than 5.4",
                new FileVersion("5.5-SNAPSHOT").compareTo(new FileVersion("5.4")) > 0);
        assertTrue("5.5 must be greater than 5.4-SNAPSHOT",
                new FileVersion("5.5").compareTo(new FileVersion("5.4-SNAPSHOT")) > 0);
        assertTrue("5.5 must be greater than 5.5-SNAPSHOT",
                new FileVersion("5.5").compareTo(new FileVersion("5.5-SNAPSHOT")) > 0);
        assertTrue("5.5-anything must be greater than 5.5",
                new FileVersion("5.5-anything").compareTo(new FileVersion("5.5")) > 0);
        assertTrue("5.5_01 must be greater than 5.5", new FileVersion("5.5_01").compareTo(new FileVersion("5.5")) > 0);
        assertTrue("5.5_01 must be lesser than 5.5.1",
                new FileVersion("5.5_01").compareTo(new FileVersion("5.5.1")) < 0);
        assertTrue("5.5-NXP must be lesser than 5.5.1",
                new FileVersion("5.5-NXP").compareTo(new FileVersion("5.5.1")) < 0);
        assertTrue("5.5-anything must be greater than 5.5-SNAPSHOT",
                new FileVersion("5.5-anything").compareTo(new FileVersion("5.5-SNAPSHOT")) > 0);
        assertTrue("5.5.1 must be greater than 5.5", new FileVersion("5.5.1").compareTo(new FileVersion("5.5")) > 0);
        assertTrue("5.5.1 must be greater than 5.5-SNAPSHOT",
                new FileVersion("5.5.1").compareTo(new FileVersion("5.5-SNAPSHOT")) > 0);
        assertTrue("5.5.1 must be greater than 5.5-anything",
                new FileVersion("5.5.1").compareTo(new FileVersion("5.5-anything")) > 0);
        assertTrue("5.5.1-SNAPSHOT must be greater than 5.5-SNAPSHOT",
                new FileVersion("5.5.1-SNAPSHOT").compareTo(new FileVersion("5.5-SNAPSHOT")) > 0);
        assertEquals("5.5-SNAPSHOT must be equal to 5.5-SNAPSHOT", new FileVersion("5.5-SNAPSHOT"), new FileVersion(
                "5.5-SNAPSHOT"));
        // Release candidate, alpha and beta versions must be lower than the
        // final version
        assertTrue("5.5 must be greater than 5.5-RC1", new FileVersion("5.5").compareTo(new FileVersion("5.5-RC1")) > 0);
        assertTrue("5.5-RC2 must be greater than 5.5-RC1",
                new FileVersion("5.5-RC2").compareTo(new FileVersion("5.5-RC1")) > 0);
        assertTrue("5.5 must be greater than 5.5-alpha1",
                new FileVersion("5.5").compareTo(new FileVersion("5.5-alpha1")) > 0);
        assertTrue("5.5 must be greater than 5.5-BETA6",
                new FileVersion("5.5").compareTo(new FileVersion("5.5-BETA6")) > 0);
        // Date-based versions must be lower than the final version
        assertTrue("5.5 must be greater than 5.5-I20120101_0115",
                new FileVersion("5.5").compareTo(new FileVersion("5.5-I20120101_0115")) > 0);
        assertTrue("5.5.1-I20120101_0115 must be greater than 5.5",
                new FileVersion("5.5.1-I20120101_0115").compareTo(new FileVersion("5.5")) > 0);
        assertTrue("5.5.1-I20120101_0115 must be greater than 5.5.1-I20110101_0115", new FileVersion(
                "5.5.1-I20120101_0115").compareTo(new FileVersion("5.5.1-I20110101_0115")) > 0);
    }

    @Test
    public void testOrdering() {
        List<FileVersion> versions = new ArrayList<>();
        versions.add(new FileVersion("5.8"));
        versions.add(new FileVersion("5.8-HF00-NXP-9999"));
        versions.add(new FileVersion("5.8-NXP-9999"));
        versions.add(new FileVersion("5.8-HF01"));
        Collections.shuffle(versions);
        Collections.sort(versions);

        List<FileVersion> expectedOrder = new ArrayList<>();
        expectedOrder.add(new FileVersion("5.8"));
        expectedOrder.add(new FileVersion("5.8-HF00-NXP-9999"));
        expectedOrder.add(new FileVersion("5.8-HF01"));
        expectedOrder.add(new FileVersion("5.8-NXP-9999"));
        assertEquals(expectedOrder, versions);
    }
}
