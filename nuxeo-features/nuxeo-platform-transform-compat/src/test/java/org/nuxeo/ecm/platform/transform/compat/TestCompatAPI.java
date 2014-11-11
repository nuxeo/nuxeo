/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.transform.compat;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.interfaces.Plugin;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.TransformServiceCommon;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;


/**
 * Verify access to converters via old TransformService API.
 *
 * @author tiry
 */
public class TestCompatAPI extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
        deployBundle("org.nuxeo.ecm.platform.transform.api");
        deployBundle("org.nuxeo.ecm.platform.transform");
    }

    protected static Blob getBlobFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return new FileBlob(file);
    }

    public void testServicePresent() throws Exception {

        Blob ooBlob = getBlobFromPath("test-docs/hello.sxw");

        TransformServiceCommon ts = Framework.getService(TransformServiceCommon.class);
        assertNotNull(ts);

        Plugin ooPlugin = ts.getPluginByName("oo2text");
        assertNotNull(ooPlugin);

        ooPlugin = ts.getPluginByMimeTypes("application/vnd.sun.xml.writer", "text/plain");
        assertNotNull(ooPlugin);
        assertEquals("oo2text", ooPlugin.getName());

        List<Plugin> plugins = ts.getPluginByDestinationMimeTypes("text/plain");
        assertNotNull(plugins);
        boolean found = false;
        for (Plugin plugin : plugins) {
            if (plugin.getName().equals("oo2text")) {
                found = true;
                break;
            }
        }
        assertTrue(found);

        List<TransformDocument> tdocs = ooPlugin.transform(null, ooBlob);
        assertNotNull(tdocs);
        assertTrue(tdocs.get(0).getBlob().getString().contains("Hello"));

        Transformer tf = ts.getTransformerByName("oo2text");
        assertNotNull(tf);

        tdocs = tf.transform(null, ooBlob);
        assertNotNull(tdocs);
        assertTrue(tdocs.get(0).getBlob().getString().contains("Hello"));
    }

}
