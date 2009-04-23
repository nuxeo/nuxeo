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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.webengine.utils.SiteConstants;

/**
 * Unit tests for the SiteActionListener.
 * @author rux
 *
 */
public class TestWebengineSiteActionListener extends RepositoryOSGITestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        openRepository();
        deployBundle("org.nuxeo.ecm.platform.webengine.sites");
    }
    
    public void testSiteActionListener() throws Exception {
        final String WorkspaceTitle = "Test Workspace";
        CoreSession session = getCoreSession();
        String id = IdUtils.generateId(WorkspaceTitle);
        DocumentModel workspace = session.createDocumentModel("/", id, "Workspace");
        workspace.setPropertyValue("dc:title", WorkspaceTitle);
        workspace = session.createDocument(workspace);
        workspace = session.saveDocument(workspace);
        session.save();
        //re-read the document model
        workspace = session.getDocument(workspace.getRef());
        String siteName = (String) workspace.getPropertyValue(
                SiteConstants.WEBCONATINER_NAME);
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

}
