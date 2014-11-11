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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
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
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = "site", guard = "user=Administrator", facets = {"Site"})
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

        return getTemplate("template_default.ftl").args(getSiteArgs(ws));
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
        try {
            Blob blob = SiteHelper.getBlob(ws, "webc:logo");
            return Response.ok().entity(blob).type(blob.getMimeType()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO return a default image
        return null;
    }

    @GET
    @Path("welcomeMedia")
    public Response getWelcomeMedia() {
        try {
            Blob blob = SiteHelper.getBlob(ws, "webc:welcomeMedia");
            return Response.ok().entity(blob).type(blob.getMimeType()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO return a default image
        return null;
    }

    protected Map<String, Object> getSiteArgs(DocumentModel doc) {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("welcomeText", SiteHelper.getString(doc, "webc:welcomeText",
                null));
        return root;
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

}
