/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.comment;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
@WebAdapter(name = CommentAdapter.NAME, type = "commentAdapter")
@Produces(MediaType.APPLICATION_JSON)
public class CommentAdapter extends DefaultAdapter {

    public static final String NAME = "comment";

    @POST
    public Response createComment(Comment comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        Comment result = commentManager.createComment(getContext().getCoreSession(), comment);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    public PartialList<Comment> getComments(@QueryParam("pageSize") @DefaultValue("0") Long pageSize,
            @QueryParam("currentPageIndex") @DefaultValue("0") Long currentPageIndex) {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getComments(getContext().getCoreSession(), doc.getId(), pageSize, currentPageIndex);
    }

    @GET
    @Path("{commentId}")
    public Comment getComment(@PathParam("commentId") String commentId) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getComment(getContext().getCoreSession(), commentId);
    }

    @GET
    @Path("external/{entityId}")
    public Comment getExternalComment(@PathParam("entityId") String entityId) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return commentManager.getExternalComment(getContext().getCoreSession(), entityId);
    }

    @PUT
    @Path("{commentId}")
    public Comment updateComment(@PathParam("commentId") String commentId, Comment comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        commentManager.updateComment(getContext().getCoreSession(), commentId, comment);
        return comment;
    }

    @PUT
    @Path("external/{entityId}")
    public Comment updateExternalComment(@PathParam("entityId") String entityId, Comment comment) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        commentManager.updateExternalComment(getContext().getCoreSession(), entityId, comment);
        return comment;
    }

    @DELETE
    @Path("{commentId}")
    public Response deleteComment(@PathParam("commentId") String commentId) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        commentManager.deleteComment(getContext().getCoreSession(), commentId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("external/{entityId}")
    public Response deleteExternalComment(@PathParam("entityId") String entityId) {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        commentManager.deleteExternalComment(getContext().getCoreSession(), entityId);
        return Response.noContent().build();
    }

}
