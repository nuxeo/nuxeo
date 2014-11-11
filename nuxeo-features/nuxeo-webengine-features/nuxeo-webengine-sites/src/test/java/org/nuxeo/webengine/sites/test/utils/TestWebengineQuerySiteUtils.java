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

import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE;

import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.service.CommentService;
import org.nuxeo.ecm.platform.comment.service.CommentServiceHelper;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;

/**
 * Unit tests for the query site utils methods.
 *
 * @author mcedica
 */
public class TestWebengineQuerySiteUtils extends SQLRepositoryTestCase {

    private static final String workspaceSiteTitle = "Test Mini Site";
    private static final String webSiteTitle = "Test Web Site";
    private static final String pageForWorkspaceSiteTitle = "Test WebPage for Workspace Site";
    private static final String pageForWebSiteTitle = "Test WebPage for Web Site";
    private static final String workspaceSiteUrl = "testMiniSiteUrl";
    private static final String webSiteUrl = "testWebSiteUrl";

    private DocumentModel workspaceSite;

    private DocumentModel webSite;

    private DocumentModel webPageForWorkspaceSite;

    private DocumentModel webPageForWebSite;

    private DocumentModel webCommentForWorkspaceSite;

    private DocumentModel webCommentForWebSite;

    public TestWebengineQuerySiteUtils() {
        super();
    }

    public TestWebengineQuerySiteUtils(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        String bundleFile = "org.nuxeo.ecm.platform.webengine.sites.tests";

        super.setUp();
        deployBundle("org.nuxeo.ecm.relations");
        deployContrib(bundleFile, "OSGI-INF/jena-test-bundle.xml");
        deployBundle("org.nuxeo.ecm.platform.comment");
        deployContrib(bundleFile, "OSGI-INF/comment-jena-contrib.xml");
        deployBundle("org.nuxeo.ecm.platform.webengine.sites.api");
        deployContrib("org.nuxeo.ecm.platform.webengine.sites.core.contrib", "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.webengine.sites.core.contrib", "OSGI-INF/permissions-contrib.xml");

        //deployBundle("org.nuxeo.ecm.platform.webengine.sites.core.contrib");

        openSession();
        initializeTestData();
    }

    @After
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        super.tearDown();
    }

    protected void createSites() throws Exception {
        String workspaceSiteId = IdUtils.generatePathSegment(workspaceSiteTitle);
        workspaceSite = session.createDocumentModel("/", workspaceSiteId,
                "WebSite");
        assertNotNull(workspaceSite);
        workspaceSite.setPropertyValue("dc:title", workspaceSiteTitle);
        workspaceSite.setPropertyValue("webc:url", workspaceSiteUrl);
        workspaceSite.setPropertyValue("webcontainer:isWebContainer", true);
        workspaceSite = session.createDocument(workspaceSite);
        //workspaceSite = session.saveDocument(workspaceSite);
        session.save();
        // re-read the document model
        workspaceSite = session.getDocument(workspaceSite.getRef());

        String webSiteId = IdUtils.generatePathSegment(webSiteTitle);
        webSite = session.createDocumentModel("/", webSiteId, "WebSite");
        assertNotNull(webSite);
        webSite.setPropertyValue("dc:title", webSiteTitle);
        webSite.setPropertyValue("webc:url", webSiteUrl);
        webSite.setPropertyValue("webcontainer:isWebContainer", true);
        webSite = session.createDocument(webSite);
        //webSite = session.saveDocument(webSite);
        session.save();
        // re-read the document model
        webSite = session.getDocument(webSite.getRef());
    }

    protected void createWebPages() throws Exception {
        webPageForWorkspaceSite = session.createDocumentModel(
                workspaceSite.getPathAsString(),
                IdUtils.generatePathSegment(pageForWorkspaceSiteTitle
                        + System.currentTimeMillis()), WEBPAGE);
        assertNotNull(webPageForWorkspaceSite);
        webPageForWorkspaceSite = session.createDocument(webPageForWorkspaceSite);
        webPageForWorkspaceSite = session.saveDocument(webPageForWorkspaceSite);
        session.save();
        // re-read the document model
        webPageForWorkspaceSite = session.getDocument(webPageForWorkspaceSite.getRef());

        webPageForWebSite = session.createDocumentModel(
                webSite.getPathAsString(),
                IdUtils.generatePathSegment(pageForWebSiteTitle
                        + System.currentTimeMillis()), WEBPAGE);
        assertNotNull(webPageForWebSite);
        webPageForWebSite = session.createDocument(webPageForWebSite);
        //webPageForWebSite = session.saveDocument(webPageForWebSite);
        session.save();
        // re-read the document model
        webPageForWebSite = session.getDocument(webPageForWebSite.getRef());
    }

    protected void createWebComments() throws Exception {
        CommentManager commentManager = getCommentManager();

        webCommentForWorkspaceSite = session.createDocumentModel("Comment");
        assertNotNull(webCommentForWorkspaceSite);
        webCommentForWorkspaceSite = commentManager.createLocatedComment(
                webPageForWorkspaceSite, webCommentForWorkspaceSite,
                workspaceSite.getPathAsString());
        session.save();

        webCommentForWebSite = session.createDocumentModel("Comment");
        assertNotNull(webCommentForWebSite);
        webCommentForWebSite = commentManager.createLocatedComment(
                webPageForWebSite, webCommentForWebSite,
                webSite.getPathAsString());
        session.save();
    }

    protected void initializeTestData() throws Exception {
        createSites();
        createWebPages();
        createWebComments();
    }

    @Test
    public void testQueryAllSites() throws Exception {
        List<DocumentModel> allSites = SiteQueriesCollection.queryAllSites(
                session, WEBSITE);
        assertEquals(2, allSites.size());
    }

    @Test
    public void testQueryAllSitesByUrl() throws Exception {
        List<DocumentModel> allWorkspaceSitesByUrlList = SiteQueriesCollection.querySitesByUrlAndDocType(
                session, workspaceSiteUrl, WEBSITE);
        assertEquals(1, allWorkspaceSitesByUrlList.size());
        List<DocumentModel> allWebSitesByUrlList = SiteQueriesCollection.querySitesByUrlAndDocType(
                session, webSiteUrl, WEBSITE);
        assertEquals(1, allWebSitesByUrlList.size());
    }

    @Test
    public void testQueryLastModifiedWebPages() throws Exception {
        List<DocumentModel> lastWorkspaceSitePages = SiteQueriesCollection.queryLastModifiedPages(
                session, workspaceSite.getPathAsString(), WEBPAGE, 5);
        assertEquals(1, lastWorkspaceSitePages.size());
        List<DocumentModel> lastWebSitePages = SiteQueriesCollection.queryLastModifiedPages(
                session, webSite.getPathAsString(), WEBPAGE, 5);
        assertEquals(1, lastWebSitePages.size());
    }

    @Test
    public void testQueryLastComments() throws Exception {
        List<DocumentModel> lastWorkspaceSiteComments = SiteQueriesCollection.queryLastComments(
                session, workspaceSite.getPathAsString(), 5, false);
        assertEquals(1, lastWorkspaceSiteComments.size());

        List<DocumentModel> lastWebSiteComments = SiteQueriesCollection.queryLastComments(
                session, webSite.getPathAsString(), 5, false);
        assertEquals(1, lastWebSiteComments.size());
    }

    protected static CommentManager getCommentManager() {
        CommentService commentService = CommentServiceHelper.getCommentService();
        CommentManager commentManager = commentService.getCommentManager();
        assertNotNull(commentManager);
        return commentManager;
    }

}
