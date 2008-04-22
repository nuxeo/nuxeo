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

package org.nuxeo.ecm.platform.site.tests.adapters;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.platform.site.adapters.DefaultSiteObjectHandler;
import org.nuxeo.ecm.platform.site.adapters.FolderishSiteObjectHandler;
import org.nuxeo.ecm.platform.site.adapters.NoteSiteObjectHandler;
import org.nuxeo.ecm.platform.site.adapters.SiteAdaptersManagerService;
import org.nuxeo.ecm.platform.site.api.SiteAdaptersManager;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.tests.fake.FakeFolderishDocumentModel;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAdapters extends NXRuntimeTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deployContrib("OSGI-INF/site-adapters-service-framework.xml");
        deployContrib("OSGI-INF/test-site-adapters-service-contrib.xml");
    }

    public void testRunTimeComponentRegistration()
    {
        SiteAdaptersManager sam = (SiteAdaptersManager) Framework.getRuntime().getComponent(SiteAdaptersManagerService.NAME);
        assertNotNull(sam);
    }

    public void testAdapterServiceRegistration()
    {
        SiteAdaptersManager sam = Framework.getLocalService(SiteAdaptersManager.class);
        assertNotNull(sam);
    }

    public void testAdapterService() throws Exception
    {
        SiteAdaptersManager sam = Framework.getLocalService(SiteAdaptersManager.class);

        DocumentModel testFile = new DocumentModelImpl("/default-domain/workspaces", "TestFile", "File");
        DocumentModel testNote = new DocumentModelImpl("/", "TestNote", "Note");

        SiteAwareObject noteAdapter =sam.getSiteAdapterForType(testNote);
        assertNotNull(noteAdapter);
        assertTrue(noteAdapter instanceof NoteSiteObjectHandler);

        SiteAwareObject fileAdapter =sam.getSiteAdapterForType(testFile);
        assertNull(fileAdapter);
    }

    public void testAdpaters() throws Exception
    {
        deployBundle("nuxeo-core-api");
        deployContrib("OSGI-INF/site-adapters-contrib.xml");

        DocumentModel testFolder = new FakeFolderishDocumentModel("/default-domain/workspaces", "TestFolder", "Folder");
        DocumentModel testFile = new DocumentModelImpl("/default-domain/workspaces", "TestFile", "File");
        DocumentModel testNote = new DocumentModelImpl("/", "TestNote", "Note");


        SiteAwareObject noteAdapter =testNote.getAdapter(SiteAwareObject.class);
        assertNotNull(noteAdapter);
        assertTrue(noteAdapter instanceof NoteSiteObjectHandler);

        SiteAwareObject fileAdapter =testFile.getAdapter(SiteAwareObject.class);
        assertNotNull(fileAdapter);
        assertTrue(fileAdapter instanceof DefaultSiteObjectHandler);

        SiteAwareObject folderAdapter =testFolder.getAdapter(SiteAwareObject.class);
        assertNotNull(folderAdapter);
        assertTrue(folderAdapter instanceof FolderishSiteObjectHandler);
    }


}
