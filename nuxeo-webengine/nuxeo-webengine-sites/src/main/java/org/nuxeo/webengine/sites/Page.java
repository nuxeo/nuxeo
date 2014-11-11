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

import static org.nuxeo.webengine.utils.SiteUtilsConstants.ALL_WEBPAGES;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.CONTEXTUAL_LINKS;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.DESCRIPTION;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.LAST_PUBLISHED_PAGES;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.NAME;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.PAGE_TITLE;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.RESULTS;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WELCOME_TEXT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentUtils;
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentsConstants;
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
        return ((Template) super.doGet()).args(getPageArguments());
    }

    @Override
    @POST
    public Response doPost() {
        String name = ctx.getForm().getString("comment");
        return null;
    }


    @GET
    @Path("numberComments")
    public int getNumberCommentsOnPage() {
        try {
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            return commentManager.getComments(getDocument()).size();
        } catch (Exception e) {
            throw WebException.wrap("Failed to get all published comments", e);
        }

    }

    public boolean isModerator() {
        try {
            CoreSession session = getCoreSession();
            return WebCommentUtils.isModeratedByCurrentUser(session,
                    getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    public boolean isModerated() {
        try {
            CoreSession session = this.getCoreSession();
            return WebCommentUtils.isCurrentModerated(session,
                    this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }


    @GET
    @Path("logo")
    public Response getLogo() {
        Response resp = null;
        try {
            DocumentModel parentWorkspace = SiteUtils.getFirstWorkspaceParent(getCoreSession(), doc);
            resp = SiteUtils.getLogoResponse(parentWorkspace);
        } catch (Exception e) {
            log.error("Unable to retrive the workspace parent. " , e);
        }
        //return a default image, maybe you want to change this in future
        if (resp == null) {
            resp = redirect(getContext().getModule().getSkinPathPrefix() +
                    "/images/logo.gif");
        }
        return resp;
    }

    @POST
    @Path("search")
    public Object getSearchParametres(
            @FormParam("searchParam") String searchParam) {
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme",
                "sites" + "/" + "search");
        Map<String, Object> root = new HashMap<String, Object>();
        try {
            DocumentModel ws = SiteUtils.getFirstWorkspaceParent(
                    getCoreSession(), doc);
            List<Object> pages = SiteUtils.searchPagesInSite(ws, searchParam,
                    50);
            root.put(RESULTS, pages);
            root.put(CONTEXTUAL_LINKS, SiteUtils.getContextualLinks(ws));
            root.put(WELCOME_TEXT, SiteHelper.getString(ws, "webc:welcomeText",
                    null));
            root.put(NAME, ws.getTitle());

            return getTemplate("template_default.ftl").args(root);

        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("createWebPage")
    public Object createWebPage() {
        try {
            CoreSession session = this.getCoreSession();

            DocumentModel createdDocument = SiteUtils.createWebPageDocument(ctx.getRequest(), session, doc.getPathAsString());

            DocumentModel webContainer = SiteUtils.getFirstWorkspaceParent(session, doc);
            String path = SiteUtils.getPagePath(webContainer, createdDocument);

            return redirect(path);

        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("modifyWebPage")
    public Object modifyWebPage() {
        try {
            CoreSession session = this.getCoreSession();
            HttpServletRequest request = ctx.getRequest();

            String title = request.getParameter("title");
            String description = request.getParameter("description");

            Boolean isRichtext = (Boolean) doc.getPropertyValue("webp:isRichtext");
            String content = null;
            if (isRichtext) {
                content = request.getParameter("richtextEditor");
            } else {
                content = request.getParameter("wikitextEditor");
            }
            String pushToMenu = request.getParameter("pushToMenu");

            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("dc:description", description);
            doc.setPropertyValue("webp:content", content);
            doc.setPropertyValue("webp:pushtomenu", Boolean.valueOf(pushToMenu));

            session.saveDocument(doc);
            session.save();

            DocumentModel webContainer = SiteUtils.getFirstWorkspaceParent(
                    session, doc);
            String path = SiteUtils.getPagePath(webContainer, doc);

            return redirect(path);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public boolean isUserWithCommentPermission() {
        try {
            CoreSession session = getCoreSession();
            return WebCommentUtils.currentUserHasCommentPermision(session,
                    getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    protected Map<String, Object> getPageArguments() {

        Map<String, Object> root = new HashMap<String, Object>();
        try {
            DocumentModel ws = SiteUtils.getFirstWorkspaceParent(
                    getCoreSession(), doc);
            root.put(PAGE_TITLE, doc.getTitle());
            root.put(NAME, SiteHelper.getString(ws, "webc:name", null));
            root.put(DESCRIPTION, SiteHelper.getString(doc, "dc:description",
                    null));
            // add web pages
            List<Object> pages = SiteUtils.getLastModifiedWebPages(doc, 5, 50);
            root.put(LAST_PUBLISHED_PAGES, pages);
            // add contextual links
            root.put(CONTEXTUAL_LINKS, SiteUtils.getContextualLinks(doc));

            // add all webpages that are directly connected to an webpage
            root.put(ALL_WEBPAGES, SiteUtils.getAllWebPages(doc));
            MimetypeRegistry mimetypeService = Framework.getService(MimetypeRegistry.class);
            root.put("mimetypeService", mimetypeService);
        } catch (Exception e) {
            log.error("Unable to get mimetype service : " + e.getMessage());
            throw WebException.wrap(e);
        }

        return root;
    }


  @GET
    @Path("publishedComments")
    public List<DocumentModel> getPublishedComments() {
        List<DocumentModel> publishedComments = new ArrayList<DocumentModel>();
        try {
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            for (DocumentModel doc : commentManager.getComments(this.getDocument())) {
                if (CommentsConstants.PUBLISHED_STATE.equals(doc.getCurrentLifeCycleState())) {
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
                if (CommentsConstants.PENDING_STATE.equals(doc.getCurrentLifeCycleState())) {
                    pendingComments.add(doc);
                }
            }
            return pendingComments;
        } catch (Exception e) {
            throw WebException.wrap("Failed to get all pending comments", e);
        }

    }

    public boolean isAposteriori() {
        try {
            return WebCommentUtils.getModerationType(
                    this.getCoreSession(), this.getDocument()).equals(WebCommentsConstants.MODERATION_APOSTERIORI);
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

}
