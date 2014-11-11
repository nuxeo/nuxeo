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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;

/**
 * @author arussel
 */
public class TestFunctions extends TestCase {

    public void testPrintFileSize() {
        assertEquals("123 kB", Functions.printFormatedFileSize("123456", "SI",
                true));
        assertEquals("1 MB", Functions.printFormatedFileSize("1000000", "SI",
                true));
        assertEquals("1 megaB", Functions.printFormatedFileSize("1000000",
                "SI", false));
        assertEquals("1 KiB", Functions.printFormatedFileSize("1024", "IEC",
                true));
        assertEquals("1 kibiB", Functions.printFormatedFileSize("1024", "IEC",
                false));
    }

    public void testPrintDuration() {
        assertEquals("3 d 2 hr", Functions.printFormattedDuration(266405));

        assertEquals("1 hr 32 min", Functions.printFormattedDuration("5533"));
        assertEquals("1 hr 32 min", Functions.printFormattedDuration(5533L));
        assertEquals("1 hr 32 min", Functions.printFormattedDuration(5533.310));

        assertEquals("3 min 13 sec", Functions.printFormattedDuration(193.4));
        assertEquals("3 min 13 sec", Functions.printFormattedDuration(193));

        assertEquals("13 sec", Functions.printFormattedDuration(13.4));

        assertEquals("0 sec", Functions.printFormattedDuration(0.01));
        assertEquals("0 sec", Functions.printFormattedDuration(0));
        assertEquals("0 sec", Functions.printFormattedDuration(null));
    }

    public void testPrintDurationi18n() {
        Map<String, String> messages = new HashMap<String, String>();
        messages.put(Functions.I18N_DURATION_PREFIX + "days", "jours");
        messages.put(Functions.I18N_DURATION_PREFIX + "hours", "heures");
        messages.put(Functions.I18N_DURATION_PREFIX + "minutes", "minutes");
        messages.put(Functions.I18N_DURATION_PREFIX + "seconds", "secondes");

        assertEquals("3 jours 2 heures", Functions.printFormattedDuration(
                266405, messages));

        assertEquals("1 heures 32 minutes", Functions.printFormattedDuration(
                "5533", messages));
        assertEquals("1 heures 32 minutes", Functions.printFormattedDuration(
                5533L, messages));
        assertEquals("1 heures 32 minutes", Functions.printFormattedDuration(
                5533.310, messages));

        assertEquals("3 minutes 13 secondes", Functions.printFormattedDuration(
                193.4, messages));
        assertEquals("3 minutes 13 secondes", Functions.printFormattedDuration(
                193.4, messages));

        assertEquals("0 secondes", Functions.printFormattedDuration(0.01,
                messages));
        assertEquals("0 secondes",
                Functions.printFormattedDuration(0, messages));
        assertEquals("0 secondes", Functions.printFormattedDuration(null,
                messages));
    }

    public void testGetFileSize() {
        assertEquals(512, Functions.getFileSize("512"));
        assertEquals(1 * 1000, Functions.getFileSize("1k"));
        assertEquals(1 * 1024, Functions.getFileSize("1Ki"));
        assertEquals(2 * 1000 * 1000, Functions.getFileSize("2m"));
        assertEquals(2 * 1024 * 1024, Functions.getFileSize("2Mi"));
        assertEquals(3L * 1000 * 1000 * 1000, Functions.getFileSize("3g"));
        assertEquals(3L * 1024 * 1024 * 1024, Functions.getFileSize("3Gi"));

        // Some bad values
        assertEquals(5 * 1024 * 1024, Functions.getFileSize("128h"));
    }
}
