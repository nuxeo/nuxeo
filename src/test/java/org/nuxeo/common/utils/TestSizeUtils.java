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
 *     Florent Guillaume
 */
package org.nuxeo.common.utils;

import junit.framework.TestCase;

public class TestSizeUtils extends TestCase {

    public long parse(String string) {
        return SizeUtils.parseSizeInBytes(string);
    }

    public void testParseSize() throws Exception {
        assertEquals(0, parse("0"));
        assertEquals(1, parse("1"));
        assertEquals(1, parse("1B"));
        assertEquals(2 * 1024, parse("2K"));
        assertEquals(2 * 1024, parse("2 kB"));
        assertEquals(3 * 1024 * 1024, parse("3M"));
        assertEquals(3 * 1024 * 1024, parse("3MB"));
        assertEquals(4L * 1024 * 1024 * 1024, parse("4G"));
        assertEquals(4L * 1024 * 1024 * 1024, parse("4Gb"));
        assertEquals(5L * 1024 * 1024 * 1024 * 1024, parse("5TB"));
        assertEquals(5L * 1024 * 1024 * 1024 * 1024, parse("5 TB"));
    }

}
