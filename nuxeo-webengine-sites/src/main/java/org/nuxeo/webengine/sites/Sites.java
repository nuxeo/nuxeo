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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

@WebObject(type = "sites", guard = "user=Administrator", facets = { "Sites" })
@Produces("text/html; charset=UTF-8")
public class Sites extends DefaultObject {

    @GET
    public Object doGet() {
        return dispatch("/");
    }

    @Path("{modulePath}")
    public Object dispatch(@PathParam("modulePath") String path) {
        try {
            if ("/".equals(path)) {
                List<Object> sites = getWebContainers();
                WebContext context = WebEngine.getActiveContext();
                CoreSession session = context.getCoreSession();
                ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/default");
                return getTemplate("list_sites.ftl").arg("sites", sites).arg(
                        "rootDoc", session.getRootDocument());
            } else {
                return newObject("site", path);
            }
        } catch (Exception e) {
            WebException.wrap(e);
        }
        return null;

    }

    public List<Object> getWebContainers() throws ClientException {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();

        // DocumentModelList list =
        // session.query("SELECT * FROM Workspace WHERE webc:isWebContainer = null");
        DocumentModelList list = session.query("SELECT * FROM Workspace");

        // filter by hand ( avoiding some core search issues )
        List<Object> sites = new ArrayList<Object>();
        for (DocumentModel d : list) {
            if (SiteHelper.getBoolean(d, "webc:isWebContainer", false)) {
                try {
                    Map<String, String> site = new HashMap<String, String>();
                    site.put("href", SiteHelper.getString(d, "webc:url"));
                    site.put("name", SiteHelper.getString(d, "webc:name"));
                    sites.add(site);
                } catch (Exception e) {
                    System.out.println("ignore site :" + d);
                }
            }
        }
        return sites;
    }

}
