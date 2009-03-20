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

import static org.nuxeo.webengine.utils.SiteUtilsConstants.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.webengine.utils.SiteUtils;
import org.nuxeo.webengine.utils.WebCommentUtils;

@WebObject(type = "site", facets = { "Site" })
@Produces("text/html; charset=UTF-8")
public class Site extends DefaultObject {

    private static final Log log = LogFactory.getLog(Site.class);

    String url;

    DocumentModel ws;

    @Override
    public void initialize(Object... args) {
        assert args != null && args.length == 1;
        url = (String) args[0];
        ws = getWorkspaceByUrl(url);
    }

    @GET
    public Object doGet() {
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/default");
        if (ws == null) {
            return getTemplate("no_site.ftl").arg("url", url);
        }
        // getting theme config from document.
        String theme = null;
        try {
            theme = (String) ws.getProperty("webpage", "theme");
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
            themePage = (String) ws.getProperty("webpage", "themePage");
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
            WebException.wrap(e);
        }
        return null;
    }

    @Path("{page}")
    public Object doGet(@PathParam("page") String page) {
        try {
            DocumentModel pageDoc = ctx.getCoreSession().getChild(ws.getRef(),
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

            return (DocumentObject) ctx.newObject(pageDoc.getType(), pageDoc);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("logo")
    public Response getLogo() {
        Response resp = null;
        try {
            Blob blob = SiteHelper.getBlob(ws, "webc:logo");
            if (blob != null) {
                resp = Response.ok().entity(blob).type(blob.getMimeType()).build();
            }
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
            Blob blob = SiteHelper.getBlob(ws, "webc:welcomeMedia");
            if (blob != null) {
                resp = Response.ok().entity(blob).type(blob.getMimeType()).build();
            }
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

    protected Map<String, Object> getSiteArguments() throws ClientException {
        Map<String, Object> root = new HashMap<String, Object>();

        root.put(WELCOME_TEXT, SiteHelper.getString(ws, "webc:welcomeText",
                null));
        root.put(SITE_NAME, SiteHelper.getString(ws, "webc:name", null));
        root.put(SITE_DESCRIPTION, SiteHelper.getString(ws, "dc:description",
                null));
        // add web pages
        root.put(LAST_PUBLISHED_PAGES, SiteUtils.getLastModifiedWebPages(
                ws, 5, 50));
        //add comments
        root.put(COMMENTS, getLastCommentsFromPages(5, 50));
        // add contextual links
        root.put(CONTEXTUAL_LINKS, SiteUtils.getContextualLinks(
                ws));
        return root;
    }

    public List<Object> getLastCommentsFromPages(int noComments,
            int noWordsFromContent) throws ClientException {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        DocumentModelList comments = session.query(
                String.format(
                        "SELECT * FROM Document WHERE "
                                + " ecm:primaryType like 'WebComment' "
                                + " AND ecm:path STARTSWITH '%s'"
                                + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:modified DESC",
                        ws.getPathAsString() + "/"), null, noComments, 0, true);
        List<Object> lastWebComments = new ArrayList<Object>();
        for (DocumentModel documentModel : comments) {
            Map<String, String> comment = new HashMap<String, String>();

            GregorianCalendar creationDate = SiteHelper.getGregorianCalendar(
                    documentModel, "webcmt:creationDate");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM",
                    getContext().getLocale());
            String formattedString = simpleDateFormat.format(creationDate.getTime());
            String[] splittedFormatterdString = formattedString.split(" ");
            comment.put("day", splittedFormatterdString[0]);
            comment.put("month", splittedFormatterdString[1]);

            try {
                comment.put("author", getUserDetails(SiteHelper.getString(
                        documentModel, "webcmt:author")));
                comment.put("pageTitle", getPageTitleForComment(documentModel));
            } catch (Exception e) {
                throw new ClientException(e);
            }
            comment.put("content", SiteHelper.getFistNWordsFromString(
                    SiteHelper.getString(documentModel, "webcmt:text"),
                    noWordsFromContent));

            lastWebComments.add(comment);

        }

        return lastWebComments;
    }

    protected DocumentModel getWorkspaceByUrl(String url) {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        try {
            DocumentModelList list = session.query(String.format(
                    "SELECT * FROM Workspace WHERE webc:url = \"%s\"", url));
            // DocumentModelList list =
            // session.query(String.format("SELECT * FROM Workspace ", url));
            if (list.size() != 0) {
                return list.get(0);
            }
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DocumentModel getWorkspace() {
        return ws;
    }

    private String getUserDetails(String username) throws Exception{
        UserManager userManager  = WebCommentUtils.getUserManager();
        NuxeoPrincipal principal = userManager.getPrincipal(username);
        if (principal == null)
            return StringUtils.EMPTY;
        if (StringUtils.isEmpty(principal.getFirstName())
                && StringUtils.isEmpty(principal.getLastName())) {
            return principal.toString();
        }
        return principal.getFirstName() + " " + principal.getLastName();
    }


    private String getPageTitleForComment(DocumentModel comment)
            throws Exception {
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        List<DocumentModel> list = commentManager.getDocumentsForComment(comment);
        if (list.size() != 0) {
            return list.get(0).getTitle();
        }
        return StringUtils.EMPTY;
    }

}
