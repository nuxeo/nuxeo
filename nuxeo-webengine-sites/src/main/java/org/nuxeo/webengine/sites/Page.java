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

import static org.nuxeo.webengine.utils.SiteConstants.*;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.utils.SiteUtils;

/**
 * Web object implementation corresponding to WebPage. It is resolved from site.
 * It holds the web page fragments back methods.
 * 
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
        String currentPerspective = (String) ctx.getRequest().getAttribute(
                "org.nuxeo.theme.perspective");
        if (StringUtils.isEmpty(currentPerspective)) {
            // Set view perspective if none present.
            ctx.getRequest().setAttribute("org.nuxeo.theme.perspective",
                    VIEW_PERSPECTIVE);
        }
        try {
            return getTemplate("template_default.ftl").args(getPageArguments());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    @POST
    public Response doPost() {
        return null;
    }

    public boolean isModerator() {
        try {
            CoreSession session = getCoreSession();
            return SiteUtils.isModeratedByCurrentUser(session, getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    // not sure it is used anymore
    public boolean isModerated() {
        try {
            CoreSession session = getCoreSession();
            return SiteUtils.isCurrentModerated(session, getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    @GET
    @Path("logo")
    public Response getLogo() {
        Response resp = null;
        try {
            DocumentModel parentWorkspace = SiteUtils.getFirstWorkspaceParent(
                    getCoreSession(), doc);
            resp = SiteUtils.getLogoResponse(parentWorkspace);
        } catch (Exception e) {
            log.error("Unable to retrive the workspace parent. ", e);
        }
        // return a default image, maybe you want to change this in future
        if (resp == null) {
            resp = redirect(getContext().getModule().getSkinPathPrefix()
                    + "/images/logo.gif");
        }
        return resp;
    }

    @POST
    @Path("search")
    public Object getSearchParametres(
            @FormParam("searchParam") String searchParam) {
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/search");
        CoreSession session = getCoreSession();
        Map<String, Object> root = new HashMap<String, Object>();
        try {
            DocumentModel ws = SiteUtils.getFirstWorkspaceParent(session, doc);
            List<Object> pages = SiteUtils.searchPagesInSite(session, ws,
                    searchParam, 50);
            root.put(RESULTS, pages);
            root.put(CONTEXTUAL_LINKS,
                    SiteUtils.getContextualLinks(session, ws));
            root.put(WELCOME_TEXT, SiteUtils.getString(ws,
                    WEBCONTAINER_WELCOMETEXT, null));
            root.put(PAGE_NAME, ws.getTitle());
            return getTemplate("template_default.ftl").args(root);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @POST
    @Path("createWebPage")
    public Object createWebPage() {
        try {
            CoreSession session = ctx.getCoreSession();

            DocumentModel createdDocument = SiteUtils.createWebPageDocument(
                    ctx.getRequest(), session, doc.getPathAsString());
            DocumentModel webContainer = SiteUtils.getFirstWorkspaceParent(
                    session, doc);
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
            CoreSession session = ctx.getCoreSession();
            HttpServletRequest request = ctx.getRequest();
            String title = request.getParameter("title");
            String description = request.getParameter("description");
            Boolean isRichtext = SiteUtils.getBoolean(doc, WEBPAGE_EDITOR,
                    false);
            String content = null;
            if (isRichtext) {
                content = request.getParameter("richtextEditorEdit");
            } else {
                content = request.getParameter("wikitextEditorEdit");
            }
            String pushToMenu = request.getParameter("pushToMenu");

            doc.setPropertyValue("dc:title", title);
            doc.setPropertyValue("dc:description", description);
            doc.setPropertyValue(WEBPAGE_CONTENT, content);
            doc.setPropertyValue(WEBPAGE_PUSHTOMENU,
                    Boolean.valueOf(pushToMenu));
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
            return SiteUtils.currentUserHasCommentPermision(session,
                    getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    protected Map<String, Object> getPageArguments() {

        Map<String, Object> root = new HashMap<String, Object>();
        CoreSession session = getCoreSession();
        try {
            DocumentModel ws = SiteUtils.getFirstWorkspaceParent(session, doc);
            root.put(PAGE_TITLE, doc.getTitle());
            root.put(PAGE_NAME,
                    SiteUtils.getString(ws, WEBCONATINER_NAME, null));
            root.put(SITE_DESCRIPTION, SiteUtils.getString(ws,
                    WEBCONTAINER_BASELINE, null));
            MimetypeRegistry mimetypeService = Framework.getService(MimetypeRegistry.class);
            root.put("mimetypeService", mimetypeService);
        } catch (Exception e) {
            log.error("Unable to get mimetype service : " + e.getMessage());
            throw WebException.wrap(e);
        }

        return root;
    }

    // not sure it is used anymore
    public boolean isAposteriori() {
        try {
            return SiteUtils.getModerationType(getCoreSession(), getDocument()).equals(
                    MODERATION_APOSTERIORI);
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

}
