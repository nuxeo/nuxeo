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

package org.nuxeo.connect.update.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FileVersionTest {

    @Test
    public void test() {
        assertArrayEquals(new FileVersion("1.3.3").getSplitVersion(),
                new Integer[] { 1, 3, 3 });
        assertEquals(new FileVersion("1.3.3").getQualifier(), "");
        assertArrayEquals(new FileVersion("1.3.3_01").getSplitVersion(),
                new Integer[] { 1, 3, 3 });
        assertEquals(new FileVersion("1.3.3_01").getQualifier(), "_01");
        assertArrayEquals(
                new FileVersion("1.1-BETA5-incubating").getSplitVersion(),
                new Integer[] { 1, 1 });
        assertEquals(new FileVersion("1.1-BETA5-incubating").getQualifier(),
                "-BETA5-incubating");
        assertArrayEquals(new FileVersion("3.3.1.GA-NX").getSplitVersion(),
                new Integer[] { 3, 3, 1 });
        assertEquals(new FileVersion("3.3.1.GA-NX").getQualifier(), ".GA-NX");
        assertArrayEquals(new FileVersion("1.3.1-NXP-7750").getSplitVersion(),
                new Integer[] { 1, 3, 1 });
        assertEquals(new FileVersion("1.3.1-NXP-7750").getQualifier(),
                "-NXP-7750");
        assertArrayEquals(new FileVersion("3.0-NX3.2").getSplitVersion(),
                new Integer[] { 3, 0 });
        assertEquals(new FileVersion("3.0-NX3.2").getQualifier(), "-NX3.2");
        assertArrayEquals(new FileVersion("0.4.0-r1096750").getSplitVersion(),
                new Integer[] { 0, 4, 0 });
        assertEquals(new FileVersion("0.4.0-r1096750").getQualifier(),
                "-r1096750");

        assertTrue("5.5 must be greater than 5.4",
                new FileVersion("5.5").compareTo(new FileVersion("5.4")) > 0);
        assertTrue("5.5-SNAPSHOT must be greater than 5.4", new FileVersion(
                "5.5-SNAPSHOT").compareTo(new FileVersion("5.4")) > 0);
        assertTrue("5.5 must be greater than 5.4-SNAPSHOT", new FileVersion(
                "5.5").compareTo(new FileVersion("5.4-SNAPSHOT")) > 0);
        assertTrue("5.5 must be greater than 5.5-SNAPSHOT", new FileVersion(
                "5.5").compareTo(new FileVersion("5.5-SNAPSHOT")) > 0);
        assertTrue("5.5-anything must be greater than 5.5", new FileVersion(
                "5.5-anything").compareTo(new FileVersion("5.5")) > 0);
        assertTrue("5.5_01 must be greater than 5.5",
                new FileVersion("5.5_01").compareTo(new FileVersion("5.5")) > 0);
        assertTrue(
                "5.5_01 must be lesser than 5.5.1",
                new FileVersion("5.5_01").compareTo(new FileVersion("5.5.1")) < 0);
        assertTrue("5.5-NXP must be lesser than 5.5.1", new FileVersion(
                "5.5-NXP").compareTo(new FileVersion("5.5.1")) < 0);
        assertTrue("5.5-anything must be greater than 5.5-SNAPSHOT",
                new FileVersion("5.5-anything").compareTo(new FileVersion(
                        "5.5-SNAPSHOT")) > 0);
        assertTrue("5.5.1 must be greater than 5.5",
                new FileVersion("5.5.1").compareTo(new FileVersion("5.5")) > 0);
        assertTrue("5.5.1 must be greater than 5.5-SNAPSHOT", new FileVersion(
                "5.5.1").compareTo(new FileVersion("5.5-SNAPSHOT")) > 0);
        assertTrue("5.5.1 must be greater than 5.5-anything", new FileVersion(
                "5.5.1").compareTo(new FileVersion("5.5-anything")) > 0);
        assertTrue("5.5.1-SNAPSHOT must be greater than 5.5-SNAPSHOT",
                new FileVersion("5.5.1-SNAPSHOT").compareTo(new FileVersion(
                        "5.5-SNAPSHOT")) > 0);
        assertEquals("5.5-SNAPSHOT must be equal to 5.5-SNAPSHOT",
                new FileVersion("5.5-SNAPSHOT"),
                new FileVersion("5.5-SNAPSHOT"));
    }
}
