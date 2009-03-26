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
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WELCOME_TEXT;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.RESULTS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.utils.SiteUtils;
import org.nuxeo.webengine.utils.WebCommentUtils;

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
            CoreSession session = this.getCoreSession();
            return WebCommentUtils.isModeratedByCurrentUser(session,
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
            e.printStackTrace();
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
            return getTemplate("template_default.ftl").args(root);

        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    public boolean isUserWithCommentPermission() {
        try {
            CoreSession session = this.getCoreSession();
            return WebCommentUtils.currentUserHasCommentPermision(session,
                    this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    protected Map<String, Object> getPageArguments() throws ClientException {

        Map<String, Object> root = new HashMap<String, Object>();

        root.put(WELCOME_TEXT, SiteHelper.getString(doc, "webp:content", null));
        root.put(DESCRIPTION, SiteHelper.getString(doc, "dc:description",
                null));
        // add web pages
        List<Object> pages = SiteUtils.getLastModifiedWebPages(
                doc, 5, 50);
        root.put(LAST_PUBLISHED_PAGES, pages);
        // add contextual links
        root.put(CONTEXTUAL_LINKS, SiteUtils.getContextualLinks(
                doc));

        // add all webpages that are directly connected to an webpage
        root.put(ALL_WEBPAGES, SiteUtils.getAllWebPages(
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
    
}
