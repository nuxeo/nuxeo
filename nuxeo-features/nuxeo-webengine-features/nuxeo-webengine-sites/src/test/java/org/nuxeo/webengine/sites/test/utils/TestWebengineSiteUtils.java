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

package org.nuxeo.webengine.sites.test.utils;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Unit tests for the utils methods.
 *
 * @author rux
 *
 */
public class TestWebengineSiteUtils extends RepositoryOSGITestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.webengine.base");
        deployBundle("org.nuxeo.ecm.platform.webengine.sites.core.contrib");
        deployBundle("org.nuxeo.ecm.platform.webengine.sites.tests");

        openRepository();
    }

    @After
    public void tearDown() throws Exception {
        releaseCoreSession();
        releaseRepository();
        super.tearDown();
    }

    private final String webSiteTitle = "Test Web Site";

    private DocumentModel webSite;

    private CoreSession session;

    protected void entryTest() throws Exception {
        session = getCoreSession();
        String id = IdUtils.generatePathSegment(webSiteTitle);
        webSite = session.createDocumentModel("/", id, "WebSite");
        webSite.setPropertyValue("dc:title", webSiteTitle);
        webSite.setPropertyValue("webcontainer:isWebContainer", true);
        webSite = session.createDocument(webSite);
        webSite = session.saveDocument(webSite);
        session.save();
        // re-read the document model
        webSite = session.getDocument(webSite.getRef());

    }

    @Test
    public void testContextualLinks() throws Exception {

        entryTest();
        DocumentModel contextualLink1 = session.createDocumentModel(
                webSite.getPathAsString(), "cl1",
                SiteConstants.CONTEXTUAL_LINK);
        contextualLink1.setPropertyValue("dc:title", "CL1");
        contextualLink1.setPropertyValue("dc:description", "CL1 description");
        contextualLink1.setPropertyValue("clink:link", "http://link1");
        contextualLink1 = session.createDocument(contextualLink1);
        contextualLink1 = session.saveDocument(contextualLink1);
        DocumentModel contextualLink2 = session.createDocumentModel(
                webSite.getPathAsString(), "cl2",
                SiteConstants.CONTEXTUAL_LINK);
        contextualLink2.setPropertyValue("dc:title", "CL2");
        contextualLink2.setPropertyValue("dc:description", "CL2 description");
        contextualLink2.setPropertyValue("clink:link", "http://link2");
        contextualLink2 = session.createDocument(contextualLink2);
        contextualLink2 = session.saveDocument(contextualLink2);

        session.save();

        DocumentModelList cLinks = session.getChildren(webSite.getRef(),
                SiteConstants.CONTEXTUAL_LINK);
        assertEquals("Don't have 2 links?", 2, cLinks.size());
        for (DocumentModel linkObject : cLinks) {
            String linkTitle = SiteUtils.getString(linkObject, "dc:title");
            String description = SiteUtils.getString(linkObject,
                    "dc:description");
            String link = SiteUtils.getString(linkObject, "clink:link");

            assertTrue("Title not correct: " + linkTitle,
                    "CL1".equals(linkTitle) || "CL2".equals(linkTitle));
            assertTrue("Description not correct: " + description,
                    "CL1 description".equals(description)
                            || "CL2 description".equals(description));
            assertTrue("Link not correct: " + description,
                    "http://link1".equals(link) || "http://link2".equals(link));
        }
    }

}
