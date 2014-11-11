/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.model.DocumentPart;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 *
 * Verify that DocumentPart.exportValues works as expected
 * (in particular for blobs)
 *
 * @author Thierry Delprat
 *
 */
public class TestDocumentPartExport extends NXRuntimeTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployContrib("org.nuxeo.ecm.core.api.tests",
                "OSGI-INF/test-blobsextractor-types-contrib.xml");
    }

    public void test() throws Exception {
        DocumentModel doc = new DocumentModelImpl("/", "doc", "ComplexDoc");

        List<Map<String, Object>> vignettes = new ArrayList<Map<String, Object>>();
        Map<String, Object> vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob1 = new ByteArrayBlob("foo1 bar1".getBytes("UTF-8"),
                "text/plain");
        blob1.setFilename("file1.txt");
        vignette.put("content", blob1);
        vignettes.add(vignette);

        vignette = new HashMap<String, Object>();
        vignette.put("width", Long.valueOf(0));
        vignette.put("height", Long.valueOf(0));
        Blob blob2 = new ByteArrayBlob("foo2 bar2".getBytes("UTF-8"),
                "text/plain");
        blob2.setFilename("file2.txt");
        vignette.put("content", blob2);
        vignettes.add(vignette);

        Map<String, Object> attachedFile = new HashMap<String, Object>();
        attachedFile.put("name", "some name");
        attachedFile.put("vignettes", vignettes);
        doc.setPropertyValue("cmpf:attachedFile", (Serializable) attachedFile);

        Blob blob3 = new ByteArrayBlob("foo3 bar3".getBytes("UTF-8"),
                "text/plain");
        doc.setProperty("file", "content", blob3);

        for (DocumentPart part : doc.getParts()) {
            Map<String, Serializable> map = part.exportValues();

            if ("file".equals(part.getName())) {
                Serializable data = map.get("file");
                boolean isBlob = true;
                if (data instanceof Blob) {
                    isBlob=true;
                }
                assertTrue(isBlob);
            }
        }
    }
}
