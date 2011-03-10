/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.webengine.sites.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBCONTAINER_URL;

public class TestSiteActionListener extends SQLRepositoryTestCase {

    protected Log log = LogFactory.getLog(TestSiteActionListener.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.webengine.sites.core.contrib",
                "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.webengine.sites.core.contrib",
                "OSGI-INF/webengine-sites-listener-contrib.xml");
        openSession();
    }

    public void testTestSiteAction() throws ClientException {
        DocumentModel website1 = session.createDocumentModel("WebSite");
        website1.setPathInfo("/", "website");
        website1 = session.createDocument(website1);
        session.save();

        DocumentModel website2 = session.createDocumentModel("WebSite");
        website2.setPathInfo("/", "website");
        website2 = session.createDocument(website2);
        session.save();

        String website1URL = (String) session.getDocument(new IdRef(website1.getId())).getPropertyValue(
                WEBCONTAINER_URL);
        assertEquals("website", website1URL);

        String website2URL = (String) session.getDocument(new IdRef(website2.getId())).getPropertyValue(
                WEBCONTAINER_URL);
        assertFalse(website1URL.equals(website2URL));
    }

    public void testTestSiteActionWithWebSiteNotInTheSameContainer()
            throws ClientException {
        DocumentModel folder1 = session.createDocumentModel("Folder");
        folder1.setPathInfo("/", "folder1");
        session.createDocument(folder1);
        session.save();

        DocumentModel folder2 = session.createDocumentModel("Folder");
        folder2.setPathInfo("/", "folder2");
        session.createDocument(folder2);
        session.save();

        DocumentModel website1 = session.createDocumentModel("WebSite");
        website1.setPathInfo("/folder1", "website");
        website1 = session.createDocument(website1);
        session.save();

        DocumentModel website2 = session.createDocumentModel("WebSite");
        website2.setPathInfo("/folder2", "website");
        website2 = session.createDocument(website2);
        session.save();

        String website1URL = (String) session.getDocument(new IdRef(website1.getId())).getPropertyValue(
                WEBCONTAINER_URL);
        assertEquals("website", website1URL);

        String website2URL = (String) session.getDocument(new IdRef(website2.getId())).getPropertyValue(
                WEBCONTAINER_URL);

        String path1 = (String) session.getDocument(website1.getRef()).getPathAsString();
        String path2 = (String) session.getDocument(website2.getRef()).getPathAsString();
        assertEquals("/folder1/website", path1);
        assertEquals("/folder2/website", path2);

        assertFalse(website1URL.equals(website2URL));
    }
}
