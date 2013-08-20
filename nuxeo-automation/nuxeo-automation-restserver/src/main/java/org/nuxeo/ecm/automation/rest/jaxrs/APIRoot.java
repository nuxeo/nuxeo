/*
 * (C) Copyright ${year} Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */

package org.nuxeo.ecm.automation.rest.jaxrs;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

/**
 * The root entry for the WebEngine module.
 *
 * @since 5.7.2
 */
@Path("/api")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "APIRoot")
public class APIRoot extends ModuleRoot {

    @GET
    public Object doGet() {
        return getView("index");
    }

    @Path("path")
    public Object getDocsByPath() {
        return new JSONDocumentRoot(ctx, "/");
    }

    @Path("id/{id}")
    public Object getDocsById(@PathParam("id")
    String id) {
        return new JSONDocumentRoot(ctx, new IdRef(id));
    }

    @Path("bulk")
    public Object getBulkDocuments(@MatrixParam("id")
    List<String> ids) throws ClientException {
        CoreSession session = getContext().getCoreSession();
//        String[] ids = new String[]{id};
        List<DocumentModel> docs = new ArrayList<>(ids.size());
        for (String loopid : ids) {
            docs.add(session.getDocument(new IdRef(loopid )));
        }


        return newObject("bulk", new DocumentModelListImpl(docs));

    }

}
