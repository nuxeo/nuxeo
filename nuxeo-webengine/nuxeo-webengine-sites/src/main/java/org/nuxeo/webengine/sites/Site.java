/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.webengine.sites;

import static org.nuxeo.webengine.utils.SiteUtilsConstants.ALL_WEBPAGES;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.COMMENTS;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.CONTEXTUAL_LINKS;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.DESCRIPTION;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.LAST_PUBLISHED_PAGES;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.NAME;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WELCOME_TEXT;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.RESULTS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.webengine.utils.SiteUtils;

@WebObject(type = "site", facets = { "Site" })
@Produces("text/html; charset=UTF-8")
public class Site extends DocumentObject {

    private static final Log log = LogFactory.getLog(Site.class);

    String url;

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        url = (String) args[0];
        doc = getSiteDocumentModelByUrl(url);
    }

    @GET
    public Object doGet() {
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/default");
        if (doc == null) {
            return getTemplate("no_site.ftl").arg("url", url);
        }
        // getting theme config from document.
        String theme = null;
        try {
            theme = (String) doc.getProperty("webpage", "theme");
        } catch (ClientException e) {
            log.error(
                    "Error while trying to display the webworkspace page. Couldn't get theme properties from the webpage",
                    e);
        }
        if (theme == null) {
            theme = "sites";
        }
        String themePage = null;
        try {
            themePage = (String) doc.getProperty("webpage", "themePage");
        } catch (ClientException e) {
            log.error(
                    "Error while trying to display the webworkspace page. Couldn't get theme properties from the webpage",
                    e);
        }
        if (themePage == null) {
            themePage = "workspace";
        }
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme",
                theme + "/" + themePage);
        try {
            return getTemplate("template_default.ftl").args(getSiteArguments());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @Path("{page}")
    public Object doGet(@PathParam("page") String page) {
        try {
            DocumentModel pageDoc = ctx.getCoreSession().getChild(doc.getRef(),
                    page);

            // getting theme config from document.
            String theme = (String) pageDoc.getProperty("webpage", "theme");
            if (theme == null) {
                theme = "sites";
            }
            String themePage = (String) pageDoc.getProperty("webpage",
                    "themePage");
            if (themePage == null) {
                themePage = "page";
            }
            ctx.getRequest().setAttribute("org.nuxeo.theme.theme",
                    theme + "/" + themePage);

            return ctx.newObject(pageDoc.getType(), pageDoc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("logo")
    public Response getLogo() {
        Response resp = null;
        try {
            resp = SiteUtils.getLogoResponse(doc);
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

    @GET
    @Path("welcomeMedia")
    public Response getWelcomeMedia() {
        Response resp = null;
        try {
            Blob blob = SiteHelper.getBlob(doc, "webc:welcomeMedia");
            if (blob != null) {
                resp = Response.ok().entity(blob).type(blob.getMimeType()).build();
            }
        } catch (Exception e) {
            log.error("Error while trying to display the website. " , e);
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
            List<Object> pages = SiteUtils.searchPagesInSite(doc, searchParam,
                    50);
            root.put(RESULTS, pages);
            root.put(CONTEXTUAL_LINKS, SiteUtils.getContextualLinks(doc));
            root.put(WELCOME_TEXT, SiteHelper.getString(doc, "webc:welcomeText",
                    null));
            root.put(NAME, SiteHelper.getString(doc, "webc:name", null));
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

            String path = SiteUtils.getPagePath(doc, createdDocument);

            return redirect(path);

        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    protected Map<String, Object> getSiteArguments() throws ClientException {
        Map<String, Object> root = new HashMap<String, Object>();

        root.put(NAME, SiteHelper.getString(doc, "webc:name", null));
        root.put(DESCRIPTION, SiteHelper.getString(doc, "dc:description",
                null));
        // add web pages
        root.put(LAST_PUBLISHED_PAGES, SiteUtils.getLastModifiedWebPages(
                doc, 5, 50));
        //add comments
        root.put(COMMENTS, SiteUtils.getLastCommentsFromPages(doc, 5, 50));
        // add contextual links
        root.put(CONTEXTUAL_LINKS, SiteUtils.getContextualLinks(
                doc));
        // add all webpages that are directly connected to an site
        root.put(ALL_WEBPAGES, SiteUtils.getAllWebPages(
                doc));

        return root;
    }

    protected DocumentModel getSiteDocumentModelByUrl(String url) {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            DocumentModelList list = session.query(String.format(
                    "SELECT * FROM Document WHERE ecm:mixinType = 'WebView' AND webc:url = \"%s\"",
                    url));
            if (list.size() != 0) {
                return list.get(0);
            }
        } catch (ClientException e) {
            log.error("Unable to retrive the webcontainer ", e);
        }
        return null;
    }

    public DocumentModel getWorkspace() {
        return doc;
    }

}
