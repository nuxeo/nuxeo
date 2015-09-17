/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service.lazy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.AutomationRenderer;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@Deploy({ "org.nuxeo.ecm.core.cache", "org.nuxeo.ecm.platform.rendition.api", "org.nuxeo.ecm.platform.rendition.core",
        "org.nuxeo.ecm.automation.core" })
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@LocalDeploy("org.nuxeo.ecm.platform.rendition.core:test-lazy-automation-rendition-contrib.xml")
/**
 *
 * Check that LazyRendition work via Nuxeo native API
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TestLazyAutomationRenditions {

    @Inject
    RenditionService rs;

    @Inject
    CoreSession session;

    @AfterClass
    public static void cleanup() throws Exception {
        //
    }

    @Test
    public void testRenditions() throws Exception {
        assertNotNull(rs);
        Rendition rendition = rs.getRendition(session.getRootDocument(), "lazyAutomation");
        assertNotNull(rendition);
        Blob blob = rendition.getBlob();
        assertEquals(0, blob.getLength());
        assertTrue(blob.getMimeType().contains("empty=true"));
        assertTrue(blob.getFilename().equals("inprogress"));
        Thread.sleep(1000);
        Framework.getService(EventService.class).waitForAsyncCompletion(5000);

        rendition = rs.getRendition(session.getRootDocument(), "lazyAutomation");
        blob = rendition.getBlob();
        assertFalse(blob.getMimeType().contains("empty=true"));
        assertEquals(session.getRootDocument().getId() + ".txt", blob.getFilename());
        String data = IOUtils.toString(blob.getStream());
        assertEquals(session.getRootDocument().getId(), data);
    }

    @Test
    public void testGetFilenameWithExtension() {
        assertEquals("a", AutomationRenderer.getFilenameWithExtension("a", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a", "text/plain", "x"));

        assertEquals("a.", AutomationRenderer.getFilenameWithExtension("a.", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.", "text/plain", "x"));

        assertEquals("a.c", AutomationRenderer.getFilenameWithExtension("a.c", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.c", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.c", "text/plain", "x"));

        assertEquals("a.ar", AutomationRenderer.getFilenameWithExtension("a.ar", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.ar", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.ar", "text/plain", "x"));

        assertEquals("a.doc", AutomationRenderer.getFilenameWithExtension("a.doc", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.doc", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.doc", "text/plain", "x"));

        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.jpeg", "text/plain", "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.smurf", "text/plain", "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.b c", "text/plain", "x"));
        assertEquals("file.txt", AutomationRenderer.getFilenameWithExtension("file", "text/plain", "x"));
    }

}
