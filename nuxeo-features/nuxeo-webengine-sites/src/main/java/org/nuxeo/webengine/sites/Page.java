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

import static org.nuxeo.webengine.sites.utils.SiteConstants.*;

import java.util.HashMap;
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
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.webengine.sites.utils.SiteUtils;

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
        ctx.getRequest().setAttribute(THEME_BUNDLE, PAGE_THEME_PAGE);
        String currentPerspective = (String) ctx.getRequest().getAttribute(
                THEME_PERSPECTIVE);
        if (StringUtils.isEmpty(currentPerspective)) {
            // Set view perspective if none present.
            ctx.getRequest().setAttribute(THEME_PERSPECTIVE,
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

    @GET
    @Path("logo")
    public Response getLogo() {
        Response resp = null;
        try {
            DocumentModel parentWebSite = SiteUtils.getFirstWebSiteParent(
                    getCoreSession(), doc);
            resp = SiteUtils.getLogoResponse(parentWebSite);
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
        ctx.getRequest().setAttribute(THEME_BUNDLE, SEARCH_THEME_PAGE);
        ctx.setProperty(SEARCH_PARAM, searchParam);
        try {
            return getTemplate("template_default.ftl").args(getPageArguments());
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
            DocumentModel webContainer = SiteUtils.getFirstWebSiteParent(
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
            DocumentModel webContainer = SiteUtils.getFirstWebSiteParent(
                    session, doc);
            String path = SiteUtils.getPagePath(webContainer, doc);
            return redirect(path);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Computes the arguments for a page. It is needed because in page some of
     * the site properties need be displayed.
     * @return
     * @throws Exception
     */
    protected Map<String, Object> getPageArguments() throws Exception {

        Map<String, Object> root = new HashMap<String, Object>();
        CoreSession session = getCoreSession();
        DocumentModel ws = SiteUtils.getFirstWebSiteParent(session, doc);
        root.put(PAGE_NAME,
                SiteUtils.getString(ws, WEBCONTAINER_NAME, null));
        root.put(SITE_DESCRIPTION, SiteUtils.getString(ws,
                WEBCONTAINER_BASELINE, null));
        MimetypeRegistry mimetypeService = Framework.getService(MimetypeRegistry.class);
        root.put("mimetypeService", mimetypeService);
        return root;
    }

}
