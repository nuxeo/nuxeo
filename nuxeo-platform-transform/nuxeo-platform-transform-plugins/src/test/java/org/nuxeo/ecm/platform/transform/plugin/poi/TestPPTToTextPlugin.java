/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.plugin.poi;

import java.io.File;
import java.util.List;

import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.timer.SimpleTimer;

public class TestPPTToTextPlugin extends AbstractPluginTestCase {

    private Plugin plugin;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        plugin = service.getPluginByName("ppt2text_poi");
    }

    @Override
    public void tearDown() throws Exception {
        plugin = null;
        super.tearDown();
    }

    public void testSmallPpt2textConversion() throws Exception {
        String path = "test-data/hello.ppt";

        SimpleTimer timer = new SimpleTimer();
        timer.start();
        List<TransformDocument> results = plugin.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path)));
        timer.stop();
        System.out.println(timer);

        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello from a Microsoft PowerPoint Presentation!",
                DocumentTestUtils.readContent(textFile));
    }

}
