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

package org.nuxeo.ecm.platform.site.tests.templates;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.site.api.SiteTemplateManager;
import org.nuxeo.ecm.platform.site.rendering.SiteTemplateManagerService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestTemplates extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("OSGI-INF/site-template-framework.xml");
        deployContrib("OSGI-INF/test-template-contrib.xml");
    }

    public void testRunTimeComponentRegistration()
    {
        SiteTemplateManagerService stm = (SiteTemplateManagerService) Framework.getRuntime().getComponent(SiteTemplateManagerService.NAME);
        assertNotNull(stm);
    }

    public void testTemplateServiceRegistration()
    {
        SiteTemplateManager stm = Framework.getLocalService(SiteTemplateManager.class);
        assertNotNull(stm);
    }

    public void testTemplateRegistration()
    {
        SiteTemplateManager stm = Framework.getLocalService(SiteTemplateManager.class);
        assertTrue(stm.getTemplateNames().contains("test"));
    }


    public void testTemplateBindingRegistration()
    {

        SiteTemplateManager stm = Framework.getLocalService(SiteTemplateManager.class);

        DocumentModel testFolder = new DocumentModelImpl("/", "TestFolder", "Folder");
        DocumentModel testFile1 = new DocumentModelImpl("/default-domain/workspaces", "TestFile1", "File");
        DocumentModel testFile2 = new DocumentModelImpl("/", "TestFile2", "File");

        String template = stm.getTemplateNameForDoc(testFolder);
        assertEquals("test", template);

        template = stm.getTemplateNameForDoc(testFile1);
        assertEquals("test", template);

        template = stm.getTemplateNameForDoc(testFile2);
        assertNull(template);
    }

}
