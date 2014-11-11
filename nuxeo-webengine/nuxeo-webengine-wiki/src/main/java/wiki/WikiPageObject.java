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
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.model.WebObject;

@WebObject(type = "WikiPage", superType="Document")
@Produces("text/html; charset=UTF-8")
public class WikiPageObject extends DocumentObject {

    @Override
    public void initialize(Object... args) {
        super.initialize(args);
        setRoot(true);
    }

    @GET
    @Path("create/{segment}")
    public Response createPage(@PathParam("segment") String segment)
            throws ClientException {
        CoreSession session = ctx.getCoreSession();
        DocumentModel newDoc = session.createDocumentModel(doc.getPathAsString(), segment, "WikiPage");
        if (newDoc.getTitle().length() == 0) {
            newDoc.getPart("dublincore").get("title").setValue(newDoc.getName());
        }
        newDoc = session.createDocument(newDoc);
        session.save();
        return redirect(path + "/" + segment);
    }

}
