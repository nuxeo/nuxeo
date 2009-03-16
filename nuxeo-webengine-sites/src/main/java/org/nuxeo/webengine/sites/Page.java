/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     stan
 */

package org.nuxeo.webengine.sites;

import static org.nuxeo.webengine.utils.SiteUtilsConstants.CONTEXTUAL_LINKS;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.LAST_PUBLISHED_PAGES;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.SITE_DESCRIPTION;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.SITE_NAME;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WELCOME_TEXT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.utils.SiteUtils;

/**
 * @author stan
 */
@WebObject(type = "WebPage", superType = "Document")
@Produces("text/html; charset=UTF-8")
public class Page extends DocumentObject {

    private static final Log log = LogFactory.getLog(Page.class);

    @Override
    @GET
    public Object doGet() {
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/page");
        try {
            return ((Template) super.doGet()).args(getPageArguments());
        } catch (ClientException e) {
            log.debug("Problems while trying to set the arguments for the Page ...");
        }
        return null;
    }

    @POST
    public Response doPost() {
        String name = ctx.getForm().getString("comment");
        return null;
    }

    @GET
    @Path("comments")
    public List<DocumentModel> getComments() {
        try {
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            return commentManager.getComments(this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to get all published comments", e);
        }

    }

    @GET
    @Path("numberComments")
    public int getNumberCommentsOnPage() {
        try {
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            return commentManager.getComments(this.getDocument()).size();
        } catch (Exception e) {
            throw WebException.wrap("Failed to get all published comments", e);
        }

    }

    public boolean isModerator() {
        try {

            return WebCommentUtils.isModeratedByCurrentUser(
                    this.getCoreSession(), this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    public boolean isUserWithCommentPermission() {
        try {
            return WebCommentUtils.currentUserHasCommentPermision(
                    this.getCoreSession(), this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    protected Map<String, Object> getPageArguments() throws ClientException {

        Map<String, Object> root = new HashMap<String, Object>();

        root.put(WELCOME_TEXT, SiteHelper.getString(doc, "webp:content", null));
        root.put(SITE_NAME, SiteHelper.getString(doc, "dc:title", null));
        root.put(SITE_DESCRIPTION, SiteHelper.getString(doc, "dc:description",
                null));
        // add web pages
        List<Object> pages = SiteUtils.getInstance().getLastModifiedWebPages(
                doc, 5, 50);
        root.put(LAST_PUBLISHED_PAGES, pages);
        // add contextual links
        root.put(CONTEXTUAL_LINKS, SiteUtils.getInstance().getContextualLinks(
                doc));
        MimetypeRegistry mimetypeService = null;
        try {
            mimetypeService = Framework.getService(MimetypeRegistry.class);
        } catch (Exception e) {
            log.error("Unable to get mimetype service : " + e.getMessage());
        }
        root.put("mimetypeService", mimetypeService);
        return root;

    }

  @GET
    @Path("publishedComments")
    public List<DocumentModel> getPublishedComments() {

        List<DocumentModel> publishedComments = new ArrayList<DocumentModel>();
        try {
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            for (DocumentModel doc : commentManager.getComments(this.getDocument())) {
                if ("moderation_published".equals(doc.getCurrentLifeCycleState())) {
                    publishedComments.add(doc);
                }
            }
            return publishedComments;
        } catch (Exception e) {
            throw WebException.wrap("Failed to get all published comments", e);
        }

    }
    
    @GET
    @Path("pendingComments")
    public List<DocumentModel> getPendingComments() {

        List<DocumentModel> pendingComments = new ArrayList<DocumentModel>();
        try {
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            for (DocumentModel doc : commentManager.getComments(this.getDocument())) {
                if ("moderation_pending".equals(doc.getCurrentLifeCycleState())) {
                    pendingComments.add(doc);
                }
            }
            return pendingComments;
        } catch (Exception e) {
            throw WebException.wrap("Failed to get all pending comments", e);
        }

    }
    
    
}
