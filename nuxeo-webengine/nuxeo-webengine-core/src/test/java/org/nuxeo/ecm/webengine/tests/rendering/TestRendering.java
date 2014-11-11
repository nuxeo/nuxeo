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

package org.nuxeo.ecm.webengine.tests.rendering;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestRendering extends NXRuntimeTestCase {

    DocumentModel site;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deployBundle("nuxeo-core-schema");
        deployBundle("nuxeo-core-query");
        deployBundle("nuxeo-core-api");
        deployBundle("nuxeo-core");
        deployBundle("nuxeo-platform-rendering");
        deployBundle("nuxeo-webengine-core");
        deployContrib("OSGI-INF/test-rendering-template-contrib.xml");
    }

    public void testX() {}
//TODO migrate tests to JAX-RS
//    public void testTemplateContribution() throws Exception {
//        WebEngine2 web = Framework.getService(WebEngine2.class);
//        WebApplication app = web.getApplication("test");
//        if (app == null) {
//            fail("Application test was not defined");
//        }
//        FreemarkerEngine rendering = (FreemarkerEngine) app.getRendering();
//        TemplateMethodModelEx tm = (TemplateMethodModelEx) rendering.getConfiguration().getSharedVariable("ext1");
//        assertEquals("My Value 1", tm.exec(null));
//        SimpleScalar t = (SimpleScalar)rendering.getConfiguration().getSharedVariable("ext2");
//        assertEquals("My Value 2", t.getAsString());
//    }

}
