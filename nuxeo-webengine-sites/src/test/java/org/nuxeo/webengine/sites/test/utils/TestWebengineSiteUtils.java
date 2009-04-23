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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.services.CommentsModerationService;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.utils.SiteConstants;
import org.nuxeo.webengine.utils.SiteUtils;

/**
 * Unit tests for the utils methods.
 * 
 * @author rux
 * 
 */
public class TestWebengineSiteUtils extends RepositoryOSGITestCase {

    private SchemaManager typeManager = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        BundleFile bundleFile = lookupBundle("org.nuxeo.ecm.platform.webengine.sites.tests");

        deployBundle("org.nuxeo.ecm.platform.webengine.sites.tests");
        deployBundle("org.nuxeo.ecm.webengine.base");

        deployContrib(bundleFile, "OSGI-INF/core-types-contrib.xml");
        deployContrib(bundleFile, "OSGI-INF/webengine-types-contrib.xml");
        deployContrib(bundleFile, "OSGI-INF/ecm-types-contrib.xml");

        openRepository();

        typeManager = NXSchema.getSchemaManager();
    }

    @Override
    public void tearDown() throws Exception {
        releaseCoreSession();
        releaseRepository();
        super.tearDown();
    }

    private final String WorkspaceTitle = "Test Mini Site";

    private DocumentModel miniSite;

    private CoreSession session;

    protected void entryTest() throws Exception {
        session = getCoreSession();
        String id = IdUtils.generateId(WorkspaceTitle);
        miniSite = session.createDocumentModel("/", id, "Workspace");
        miniSite.setPropertyValue("dc:title", WorkspaceTitle);
        miniSite.setPropertyValue("webcontainer:isWebContainer", new Boolean(
                true));
        miniSite = session.createDocument(miniSite);
        miniSite = session.saveDocument(miniSite);
        session.save();
        // re-read the document model
        miniSite = session.getDocument(miniSite.getRef());

    }

    public void testContextualLinks() throws Exception {

        entryTest();
        DocumentModel contextualLink1 = session.createDocumentModel(
                miniSite.getPathAsString(), "cl1",
                SiteConstants.CONTEXTUAL_LINK);
        contextualLink1.setPropertyValue("dc:title", "CL1");
        contextualLink1.setPropertyValue("dc:description", "CL1 description");
        contextualLink1.setPropertyValue("clink:link", "http://link1");
        contextualLink1 = session.createDocument(contextualLink1);
        contextualLink1 = session.saveDocument(contextualLink1);
        DocumentModel contextualLink2 = session.createDocumentModel(
                miniSite.getPathAsString(), "cl2",
                SiteConstants.CONTEXTUAL_LINK);
        contextualLink2.setPropertyValue("dc:title", "CL2");
        contextualLink2.setPropertyValue("dc:description", "CL2 description");
        contextualLink2.setPropertyValue("clink:link", "http://link2");
        contextualLink2 = session.createDocument(contextualLink2);
        contextualLink2 = session.saveDocument(contextualLink2);

        session.save();

        List<Object> cLinks = SiteUtils.getContextualLinks(session, miniSite);
        assertTrue("Don't have 2 links?", cLinks.size() == 2);
        for (Object linkObject : cLinks) {
            Map<String, String> mapLink = (Map<String, String>) linkObject;
            String linkTitle = mapLink.get("title");
            String description = mapLink.get("description");
            String link = mapLink.get("link");

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
