/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *     stan
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest;

import java.util.Date;

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
import org.nuxeo.ecm.platform.comment.workflow.services.CommentsModerationService;
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
 * <li>POST - create a new comment
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 * @author rux allow extending the service for the possible customizations. Some atomic actions are provided with
 *         default implementation but allowed for overwriting.
 * @deprecated since 10.3, use {@link org.nuxeo.ecm.restapi.server.jaxrs.comment.CommentAdapter} instead.
 */
@Deprecated
@WebAdapter(name = "comments", type = "CommentService", targetType = "Document", targetFacets = { "Commentable" })
public class CommentService extends DefaultAdapter {

    @POST
    public Response doPost(@FormParam("text") String cText) {
        if (cText == null) {
            throw new IllegalParameterException("Expecting a 'text' parameter");
        }
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        DocumentModel comment = session.createDocumentModel("Comment");
        comment.setPropertyValue("comment:author", session.getPrincipal().getName());
        comment.setPropertyValue("comment:text", cText);
        comment.setPropertyValue("comment:creationDate", new Date());
        comment = createCommentDocument(session, pageDoc, comment);
        session.save();
        publishComment(session, pageDoc, comment);

        return redirect(getTarget().getPath());
    }

    @GET
    @Path("reject")
    public Response reject() {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        FormData form = ctx.getForm();
        String commentId = form.getString(FormData.PROPERTY);
        DocumentModel comment = session.getDocument(new IdRef(commentId));
        rejectComment(session, pageDoc, comment);
        return redirect(dobj.getPath());
    }

    @GET
    @Path("approve")
    public Response approve() {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        FormData form = ctx.getForm();
        String commentId = form.getString(FormData.PROPERTY);
        DocumentModel comment = session.getDocument(new IdRef(commentId));
        approveComent(session, pageDoc, comment);
        return redirect(dobj.getPath());
    }

    @GET
    @Path("delete")
    public Response remove() {
        return deleteComment();
    }

    @DELETE
    public Response deleteComment() {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        FormData form = ctx.getForm();
        String docId = form.getString(FormData.PROPERTY);
        DocumentModel comment = session.getDocument(new IdRef(docId));
        deleteComment(dobj.getDocument(), comment);
        return redirect(dobj.getPath());
    }

    public static CommentManager getCommentManager() {
        return Framework.getService(CommentManager.class);
    }

    public static CommentsModerationService getCommentsModerationService() {
        return Framework.getService(CommentsModerationService.class);
    }

    /**
     * Can be overwritten to allow creation of localized comment. Defaults to create comment in comments root.
     *
     * @param session the core session
     * @param target commented document
     * @param comment comment itself
     * @return the comment created
     */
    protected DocumentModel createCommentDocument(CoreSession session, DocumentModel target, DocumentModel comment) {
        return getCommentManager().createComment(target, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to publish right away.
     *
     * @param session the core session
     * @param target commented document
     * @param comment comment itself
     */
    protected void publishComment(CoreSession session, DocumentModel target, DocumentModel comment) {
        getCommentsModerationService().publishComment(session, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to delete right away.
     *
     * @param target commented document
     * @param comment comment itself
     */
    protected void deleteComment(DocumentModel target, DocumentModel comment) {
        getCommentManager().deleteComment(target, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to reject and delete right away.
     *
     * @param target commented document
     * @param comment comment itself
     */
    protected void rejectComment(CoreSession session, DocumentModel target, DocumentModel comment) {
        getCommentsModerationService().rejectComment(session, target, comment.getId());
        getCommentManager().deleteComment(target, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to approve right away.
     *
     * @param target commented document
     * @param comment comment itself
     */
    protected void approveComent(CoreSession session, DocumentModel target, DocumentModel comment) {
        getCommentsModerationService().approveComent(session, target, comment.getId());
    }

}
