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

package wiki;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

@Path("/wikis")
@WebObject(type = "wikis", facets = { "mainWiki" })
@Produces("text/html; charset=UTF-8")
public class Main extends ModuleRoot {

    public Main() {
    }

    @GET
    public Object doGet() throws ClientException {
        CoreSession session = ctx.getCoreSession();

        DocumentModelList wikiSites = session.query("SELECT * FROM Wiki WHERE ecm:currentLifeCycleState != 'deleted' ");

        return getView("index").arg("wikis", wikiSites);
    }

    @Path("{segment}")
    public Object getWiki(@PathParam("segment") String segment) {
        try {
            CoreSession session = ctx.getCoreSession();
            DocumentModel wikiSite = session.getDocument(new IdRef(segment));
            return newObject("Wiki", wikiSite.getId());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    // handle errors
    @Override
    public Object handleError(WebApplicationException e) {
        if (e instanceof WebSecurityException) {
            return Response.status(401).entity(
                    getTemplate("error/error_401.ftl")).build();
        } else if (e instanceof WebResourceNotFoundException) {
            return Response.status(404).entity(
                    getTemplate("error/error_404.ftl")).build();
        } else {
            return super.handleError(e);
        }
    }

    @Override
    public String getLink(DocumentModel doc) {
        String type = doc.getType();
        if ("Wiki".equals(type)) {
            return getPath() + "/" + doc.getName();
        } else if ("WikiPage".equals(type)) {
            // TODO: this will not work with multi level wiki pages
            org.nuxeo.common.utils.Path path = doc.getPath();
            int cnt = path.segmentCount();
            String s = getPath() + "/" + path.segment(cnt - 2) + "/"
                    + path.lastSegment();
            return s;
        }
        return super.getLink(doc);
    }

}
