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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.webengine.utils.*;

/**
 * @author stan
 */
@WebObject(type = "WebPage", superType = "Document")
@Produces("text/html; charset=UTF-8")
public class Page extends DocumentObject {

    @Override
    @GET
    public Object doGet() {
        ctx.getRequest().setAttribute("org.nuxeo.theme.theme", "sites/page");
        // ctx.getRequest().setAttribute("theme", "sites/page");
        return super.doGet();
    }

    @POST
    public Response doPost() {
        String name = ctx.getForm().getString("comment");
        return null;
    }

    @GET
    @Path("comments")
    public List<DocumentModel> getComments() {
        CommentableDocument cDoc = this.getDocument().getAdapter(
                CommentableDocument.class, true);
        try {
            return cDoc.getComments();
        } catch (ClientException e) {
            throw WebException.wrap("Failed to get all published comments", e);
        }

    }

    @GET
    @Path("noComments")
    public int getNoComments() {
        CommentableDocument cDoc = this.getDocument().getAdapter(
                CommentableDocument.class, true);
        try {
            return cDoc.getComments().size();
        } catch (ClientException e) {
            throw WebException.wrap("Failed to get all published comments", e);
        }

    }

    public boolean isModerator() {
        try {

            return WebCommentUtils.isModeratedByCurrentUser(
                    this.getCoreSession(), this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }

    public boolean isUserWithCommentPermission() {
        try {

            return WebCommentUtils.currentUserHasCommentPermision(
                    this.getCoreSession(), this.getDocument());
        } catch (Exception e) {
            throw WebException.wrap("Failed to delete comment", e);
        }
    }


}
