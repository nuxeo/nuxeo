/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.ObjectData;
import org.apache.chemistry.opencmis.commons.data.RenditionData;
import org.apache.chemistry.opencmis.commons.spi.ObjectService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.opencmis.impl.server.NuxeoCmisService;
import org.nuxeo.ecm.core.opencmis.tests.Helper;

public class TestNuxeoBindingRenditions extends NuxeoBindingTestCase {

    protected ObjectService objService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Helper.makeNuxeoRepository(nuxeotc.session);
        sleepForFulltext();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Override
    protected void deployBundles() throws Exception {
        super.deployBundles();
        nuxeotc.deployBundle("org.nuxeo.ecm.core.convert.api");
        nuxeotc.deployBundle("org.nuxeo.ecm.core.convert");
        nuxeotc.deployBundle("org.nuxeo.ecm.core.convert.plugins");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.convert");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.rendition.api");
        nuxeotc.deployBundle("org.nuxeo.ecm.platform.rendition.core");
        nuxeotc.deployBundle("org.nuxeo.ecm.automation.core");
        // start OOoManagerComponent
        nuxeotc.fireFrameworkStarted();
    }

    @Override
    public void init() throws Exception {
        super.init();
        objService = binding.getObjectService();
    }

    protected ObjectData getObjectByPath(String path) {
        return objService.getObjectByPath(repositoryId, path, null, null, null,
                null, null, null, null);
    }

    public static final Comparator<RenditionData> RENDITION_CMP = new Comparator<RenditionData>() {
        @Override
        public int compare(RenditionData a, RenditionData b) {
            return a.getStreamId().compareTo(b.getStreamId());
        };
    };

    public static final int TEXT_PNG_ICON_SIZE = 394;

    @Test
    public void testRenditions() throws Exception {
        ObjectData ob = getObjectByPath("/testfolder1/testfile1");

        // list renditions
        List<RenditionData> renditions = objService.getRenditions(repositoryId,
                ob.getId(), null, null, null, null);
        assertEquals(2, renditions.size());
        Collections.sort(renditions, RENDITION_CMP);

        RenditionData ren;
        ren = renditions.get(0);
        assertEquals("cmis:thumbnail", ren.getKind());
        assertEquals("nuxeo:icon", ren.getStreamId());
        assertEquals("image/png", ren.getMimeType());
        assertEquals("text.png", ren.getTitle());

        ren = renditions.get(1);
        assertEquals("nuxeo:rendition", ren.getKind());
        assertEquals("nuxeo:rendition:pdf", ren.getStreamId());
        assertEquals("application/pdf", ren.getMimeType());
        assertEquals("label.rendition.pdf", ren.getTitle());

        ContentStream cs;
        cs = objService.getContentStream(repositoryId, ob.getId(),
                "nuxeo:icon", null, null, null);
        assertEquals("image/png", cs.getMimeType());
        assertEquals("text.png", cs.getFileName());
        assertEquals(TEXT_PNG_ICON_SIZE, cs.getBigLength().longValue());

        cs = objService.getContentStream(repositoryId, ob.getId(),
                "nuxeo:rendition:pdf", null, null, null);
        assertEquals("application/pdf", cs.getMimeType());
        assertEquals("testfile1_Title.pdf", cs.getFileName());
    }

    @Test
    public void testFilenameWithExt() {
        assertEquals("a.x", NuxeoCmisService.filenameWithExt("a.", "x"));
        assertEquals("a.x", NuxeoCmisService.filenameWithExt("a.c", "x"));
        assertEquals("a.x", NuxeoCmisService.filenameWithExt("a.ar", "x"));
        assertEquals("a.x", NuxeoCmisService.filenameWithExt("a.doc", "x"));
        assertEquals("a.x", NuxeoCmisService.filenameWithExt("a.jpeg", "x"));
        assertEquals("a.smurf.x",
                NuxeoCmisService.filenameWithExt("a.smurf", "x"));
        assertEquals("a.b c.x", NuxeoCmisService.filenameWithExt("a.b c", "x"));
        assertEquals("a.x", NuxeoCmisService.filenameWithExt("a", "x"));
        assertEquals("file.x", NuxeoCmisService.filenameWithExt("file", "x"));
    }

}
