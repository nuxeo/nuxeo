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

import java.util.List;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteQueriesColection;

/**
 * Unit tests for the query site utils methods.
 * 
 * @author mcedica
 * 
 */
public class TestWebengineQuerySiteUtils extends SQLRepositoryTestCase {

    private final String WorkspaceTitle = "Test Mini Site";

    private final String PageTitle = "Test Web Page";

    private final String WorkspaceUrl = "testUrl";

    private DocumentModel miniSite;

    private DocumentModel webPage;

    private DocumentModel webComment;

    public TestWebengineQuerySiteUtils(String name) {
        super(name);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setUp() throws Exception {
        String bundleFile = "org.nuxeo.ecm.platform.webengine.sites.tests";

        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployContrib(bundleFile, "OSGI-INF/jena-test-bundle.xml");
        deployBundle("org.nuxeo.ecm.platform.comment");
        deployBundle("org.nuxeo.ecm.platform.comment.core");
        deployContrib(bundleFile, "OSGI-INF/comment-jena-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.comment",
                "OSGI-INF/CommentService.xml");
        deployContrib(bundleFile, "OSGI-INF/RepositoryService.xml");
        deployContrib(bundleFile, "OSGI-INF/RepositoryManager.xml");
        deployContrib(bundleFile, "OSGI-INF/core-types-contrib.xml");
        deployContrib(bundleFile, "OSGI-INF/ecm-types-contrib.xml");

        openSession();
        initializeTestData();
    }

    @Override
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    protected void createWebSite() throws Exception {
        String id = IdUtils.generateId(WorkspaceTitle);
        miniSite = session.createDocumentModel("/", id, "Workspace");
        assertNotNull(miniSite);
        miniSite.setPropertyValue("dc:title", WorkspaceTitle);
        miniSite.setPropertyValue("webc:url", WorkspaceUrl);
        miniSite.setPropertyValue("webcontainer:isWebContainer", new Boolean(
                true));
        miniSite = session.createDocument(miniSite);
        miniSite = session.saveDocument(miniSite);
        session.save();
        // re-read the document model
        miniSite = session.getDocument(miniSite.getRef());
    }

    protected void createWebPage() throws Exception {
        webPage = session.createDocumentModel(miniSite.getPathAsString(),
                IdUtils.generateId(PageTitle + System.currentTimeMillis()),
                SiteConstants.WEBPAGE);
        assertNotNull(webPage);
        webPage = session.createDocument(webPage);
        webPage = session.saveDocument(webPage);
        session.save();
    }

    protected void createWebComment() throws Exception {
        webComment = session.createDocumentModel("Comment");
        assertNotNull(webComment);
        CommentManager commentManager = getCommentManager();
        webComment = commentManager.createComment(webPage, webComment);
        session.save();
    }

    protected void initializeTestData() throws Exception {
        createWebSite();
        createWebPage();
        createWebComment();
    }

    public void testQueryAllSites() throws Exception {
        List<DocumentModel> allSites = SiteQueriesColection.queryAllSites(session);
        assertEquals(1, allSites.size());
    }

    public void testQueryAllSitesByUrl() throws Exception {
        List<DocumentModel> allSitesByUrlList = SiteQueriesColection.querySitesByUrl(
                session, WorkspaceUrl);
        assertEquals(1, allSitesByUrlList.size());
    }

    public void testQueryLastModifiedWebPages() throws Exception {
        List<DocumentModel> lastPages = SiteQueriesColection.queryLastModifiedPages(
                session, miniSite.getPathAsString(), 5);
        assertEquals(1, lastPages.size());
    }

    public void testQueryLastComments() throws Exception {
        List<DocumentModel> lasComments = SiteQueriesColection.queryLastComments(
                session, miniSite.getPathAsString(), 5, false);
        assertEquals(1, lasComments.size());

    }

    protected CommentManager getCommentManager() throws Exception {
        CommentService commentService = CommentServiceHelper.getCommentService();
        CommentManager commentManager = commentService.getCommentManager();
        assertNotNull(commentManager);
        return commentManager;
    }

}
