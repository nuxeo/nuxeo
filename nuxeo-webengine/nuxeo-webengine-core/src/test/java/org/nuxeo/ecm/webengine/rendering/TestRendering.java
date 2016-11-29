/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.rendering;

import org.junit.Before;
import org.junit.Ignore;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

@Ignore
public class TestRendering extends NXRuntimeTestCase {

    DocumentModel site;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.query");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.rendering");
        deployBundle("org.nuxeo.ecm.webengine.core");

        // deployContrib("org.nuxeo.ecm.webengine.core.tests", "test-rendering-template-contrib.xml");
    }

    public void testX() {
    }
    // TODO migrate tests to JAX-RS
    // public void testTemplateContribution() throws Exception {
    // WebEngine2 web = Framework.getService(WebEngine2.class);
    // WebApplication app = web.getApplication("test");
    // if (app == null) {
    // fail("Application test was not defined");
    // }
    // FreemarkerEngine rendering = (FreemarkerEngine) app.getRendering();
    // TemplateMethodModelEx tm = (TemplateMethodModelEx) rendering.getConfiguration().getSharedVariable("ext1");
    // assertEquals("My Value 1", tm.exec(null));
    // SimpleScalar t = (SimpleScalar)rendering.getConfiguration().getSharedVariable("ext2");
    // assertEquals("My Value 2", t.getAsString());
    // }

}
