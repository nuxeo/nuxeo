/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *     stan
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import java.util.Date;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.forms.FormData;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Comment Service - manages document comments.
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li> POST - create a new comment
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 */
@WebAdapter(name = "comments", type = "CommentService", targetType = "Document", targetFacets = {"Commentable"})
public class CommentService extends DefaultAdapter {

    @POST
    public Response doPost(@FormParam("text") String cText) {
        if (cText == null) {
            throw new IllegalParameterException("Expecting a 'text' parameter");
        }

        DocumentObject dobj = (DocumentObject) getTarget();
        CommentManager commentManager = getCommentManager();
        CoreSession session = dobj.getCoreSession();
        try {
            // create a new webComment on this page
            DocumentModel webComment = session.createDocumentModel("WebComment");
            webComment.setPropertyValue("webcmt:author", session.getPrincipal().getName());
            webComment.setPropertyValue("webcmt:text", cText);
            webComment.setPropertyValue("webcmt:creationDate", new Date());
            webComment = commentManager.createLocatedComment(
                    dobj.getDocument(), webComment, getParentWorkspacePath(session, dobj.getDocument()));
            session.save();
            return redirect(getTarget().getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("delete")
    public Response remove() {
        try {
            return deleteComment();
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    @DELETE
    public Response deleteComment() throws Exception {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        FormData form = ctx.getForm();
        String docId = form.getString(FormData.PROPERTY);
        DocumentModel comment = session.getDocument(new IdRef(docId));
        CommentManager commentManager = getCommentManager();
        commentManager.deleteComment(dobj.getDocument(), comment);
        return redirect(dobj.getPath());
    }

    public static CommentManager getCommentManager() {
        return Framework.getLocalService(CommentManager.class);
    }

    public static String getParentWorkspacePath(CoreSession session, DocumentModel doc)
            throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel documentModel : parents) {
            if (documentModel.getType().equals("Workspace")) {
                return documentModel.getPathAsString();
            }
        }
        return null;
    }

}
