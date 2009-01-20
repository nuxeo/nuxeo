/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.ui.web.util;

import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;

import junit.framework.TestCase;

/**
 * @author arussel
 *
 */
public class TestFunctions extends TestCase {
    public void testPrintFileSize() {
        assertEquals("123 kB", Functions.printFormatedFileSize("123456", "SI", true));
        assertEquals("1 MB", Functions.printFormatedFileSize("1000000", "SI", true));
        assertEquals("1 megaB", Functions.printFormatedFileSize("1000000", "SI", false));
        assertEquals("1 KiB", Functions.printFormatedFileSize("1024", "IEC", true));
        assertEquals("1 kibiB", Functions.printFormatedFileSize("1024", "IEC", false));
    }
}
