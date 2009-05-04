/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.webengine.sites.test.listeners;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.webengine.sites.utils.SiteConstants;

/**
 * Unit tests for the SiteActionListener.
 * @author rux
 *
 */
public class TestWebengineSiteActionListener extends SQLRepositoryTestCase {

    public TestWebengineSiteActionListener(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        String bundleFile = "org.nuxeo.ecm.platform.webengine.sites.tests";

        super.setUp();
        deployContrib(bundleFile, "OSGI-INF/listener-contrib.xml");
        deployContrib(bundleFile, "OSGI-INF/core-types-contrib.xml");
        deployContrib(bundleFile, "OSGI-INF/ecm-types-contrib.xml");

        openSession();
    }

    public void testSiteActionListenerWorkspace() throws Exception {
        final String WorkspaceTitle = "Test Workspace";
        String id = IdUtils.generateId(WorkspaceTitle);
        DocumentModel workspace = session.createDocumentModel("/", id, "Workspace");
        workspace.setPropertyValue("dc:title", WorkspaceTitle);
        workspace = session.createDocument(workspace);
        workspace = session.saveDocument(workspace);
        session.save();
        //re-read the document model
        workspace = session.getDocument(workspace.getRef());
        String siteName = (String) workspace.getPropertyValue(
                SiteConstants.WEBCONTAINER_NAME);
        String siteUrl = (String) workspace.getPropertyValue(
                SiteConstants.WEBCONTAINER_URL);
        String documentTitle = (String) workspace.getTitle();
        String documentName = (String) workspace.getName();
        assertFalse("No name in site?", StringUtils.isBlank(siteName));
        assertFalse("No url in site?", StringUtils.isBlank(siteUrl));
        assertFalse("No name in document?", StringUtils.isBlank(documentName));
        assertFalse("No title in document?", StringUtils.isBlank(documentTitle));
        //name contains the title
        assertTrue("Name not valid for web container: " + siteName,
                documentTitle.equals(siteName));
        //url contains the name
        assertTrue("URL not valid for web container: " + siteUrl,
                documentName.equals(siteUrl));
    }

    public void testSiteActionListenerWebSite() throws Exception {
        final String webSiteTitle = "Test WebSite";
        String id = IdUtils.generateId(webSiteTitle);
        DocumentModel webSite = session.createDocumentModel("/", id, "WebSite");
        webSite.setPropertyValue("dc:title", webSiteTitle);
        webSite = session.createDocument(webSite);
        webSite = session.saveDocument(webSite);
        session.save();
        //re-read the document model
        webSite = session.getDocument(webSite.getRef());
        String siteName = (String) webSite.getPropertyValue(
                SiteConstants.WEBCONTAINER_NAME);
        String siteUrl = (String) webSite.getPropertyValue(
                SiteConstants.WEBCONTAINER_URL);
        String documentTitle = (String) webSite.getTitle();
        String documentName = (String) webSite.getName();
        assertFalse("No name in site?", StringUtils.isBlank(siteName));
        assertFalse("No url in site?", StringUtils.isBlank(siteUrl));
        assertFalse("No name in document?", StringUtils.isBlank(documentName));
        assertFalse("No title in document?", StringUtils.isBlank(documentTitle));
        //name contains the title
        assertTrue("Name not valid for web container: " + siteName,
                documentTitle.equals(siteName));
        //url contains the name
        assertTrue("URL not valid for web container: " + siteUrl,
                documentName.equals(siteUrl));
    }

}
