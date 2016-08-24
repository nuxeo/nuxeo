/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.ui.web.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.ui.web.tag.fn.Functions;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author arussel
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy({ "org.nuxeo.ecm.platform.ui:OSGI-INF/jsfconfiguration-properties.xml" })
public class TestFunctions {

    @Inject
    protected HotDeployer deployer;

    @Test
    public void testPrintFileSize() throws Exception {
        String bytePrefixFormat = Functions.getDefaultBytePrefix().name();
        assertEquals(Functions.DEFAULT_BYTE_PREFIX_FORMAT, bytePrefixFormat);
        assertEquals("123 kB", Functions.printFileSize("123456"));

        deployer.deploy("@org.nuxeo.ecm.platform.ui:OSGI-INF/print-jsfconfiguration-test-contrib.xml");
        assertEquals("120 KB", Functions.printFileSize("123456"));

        deployer.deploy("@org.nuxeo.ecm.platform.ui:OSGI-INF/print-jsfconfiguration-test-override-contrib.xml");
        assertEquals("120 KiB", Functions.printFileSize("123456"));
    }

    @Test
    public void testPrintFormatedFileSize() {
        assertEquals("123 kB", Functions.printFormatedFileSize("123456", "SI", true));
        assertEquals("1 MB", Functions.printFormatedFileSize("1000000", "SI", true));
        assertEquals("1 megaB", Functions.printFormatedFileSize("1000000", "SI", false));
        assertEquals("1 KiB", Functions.printFormatedFileSize("1024", "IEC", true));
        assertEquals("1 kibiB", Functions.printFormatedFileSize("1024", "IEC", false));
    }

    @Test
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

    @Test
    public void testPrintDurationi18n() {
        Map<String, String> messages = new HashMap<String, String>();
        messages.put(Functions.I18N_DURATION_PREFIX + "days", "jours");
        messages.put(Functions.I18N_DURATION_PREFIX + "hours", "heures");
        messages.put(Functions.I18N_DURATION_PREFIX + "minutes", "minutes");
        messages.put(Functions.I18N_DURATION_PREFIX + "seconds", "secondes");

        assertEquals("3 jours 2 heures", Functions.printFormattedDuration(266405, messages));

        assertEquals("1 heures 32 minutes", Functions.printFormattedDuration("5533", messages));
        assertEquals("1 heures 32 minutes", Functions.printFormattedDuration(5533L, messages));
        assertEquals("1 heures 32 minutes", Functions.printFormattedDuration(5533.310, messages));

        assertEquals("3 minutes 13 secondes", Functions.printFormattedDuration(193.4, messages));
        assertEquals("3 minutes 13 secondes", Functions.printFormattedDuration(193.4, messages));

        assertEquals("0 secondes", Functions.printFormattedDuration(0.01, messages));
        assertEquals("0 secondes", Functions.printFormattedDuration(0, messages));
        assertEquals("0 secondes", Functions.printFormattedDuration(null, messages));
    }

    @Test
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

    @Test
    public void testFileSize() {
        assertEquals("3 GB", Functions.printFormatedFileSize("3145728000", "SI", true));
    }

    @Test
    public void testGenerateValidId() throws Exception {
        assertNull(Functions.jsfTagIdEscape(null));
        assertEquals("", Functions.jsfTagIdEscape(""));
        assertEquals("blah_blah", Functions.jsfTagIdEscape("blah_blah"));
        assertEquals("blah_blah", Functions.jsfTagIdEscape("blah blah"));
        assertEquals("blah_blah", Functions.jsfTagIdEscape("blah-blah"));
        assertEquals("blah_blahe", Functions.jsfTagIdEscape("blah_blahé"));
    }

    @Test
    public void testJoinRender() {
        assertEquals("", Functions.joinRender(null, null));
        assertEquals("", Functions.joinRender(null, ""));
        assertEquals("", Functions.joinRender("", null));
        assertEquals("", Functions.joinRender(" ", "    "));
        assertEquals("foo", Functions.joinRender("foo   ", "    "));
        assertEquals("foo bar", Functions.joinRender("foo", "bar"));
        assertEquals("foo bar baz", Functions.joinRender(" foo", "bar    baz   "));
    }

}
