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

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
 * <li>POST - create a new comment
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:stan@nuxeo.com">Sun Seng David TAN</a>
 * @author rux allow extending the service for the possible customizations. Some
 * atomic actions are provided with default implementation but allowed for
 * overwriting.
 */
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
        try {

            DocumentModel comment = session.createDocumentModel("Comment");
            comment.setPropertyValue("comment:author",
                    session.getPrincipal().getName());
            comment.setPropertyValue("comment:text", cText);
            comment.setPropertyValue("comment:creationDate", new Date());
            comment = createCommentDocument(session, pageDoc, comment);
            session.save();
            publishComment(session, pageDoc, comment);

            return redirect(getTarget().getPath());
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    @Path("reject")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Response reject() {
        try {
            DocumentObject dobj = (DocumentObject) getTarget();
            CoreSession session = dobj.getCoreSession();
            DocumentModel pageDoc = dobj.getDocument();
            FormData form = ctx.getForm();
            String commentId = form.getString(FormData.PROPERTY);
            DocumentModel comment = session.getDocument(new IdRef(commentId));
            rejectComment(session, pageDoc, comment);
            return redirect(dobj.getPath());
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw WebException.wrap("Failed to reject comment", e);
        }
    }

    @GET
    @Path("approve")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Response approve() {
        try {
            DocumentObject dobj = (DocumentObject) getTarget();
            CoreSession session = dobj.getCoreSession();
            DocumentModel pageDoc = dobj.getDocument();
            FormData form = ctx.getForm();
            String commentId = form.getString(FormData.PROPERTY);
            DocumentModel comment = session.getDocument(new IdRef(commentId));
            approveComent(session, pageDoc, comment);
            return redirect(dobj.getPath());
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            throw WebException.wrap("Failed to approve comment", e);
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
        deleteComment(dobj.getDocument(), comment);
        return redirect(dobj.getPath());
    }

    public static CommentManager getCommentManager() throws Exception {
        CommentManager commentManager = Framework.getService(CommentManager.class);
        if (commentManager == null) {
            throw new WebException("Unable to get commentManager");
        }
        return commentManager;
    }

    public static CommentsModerationService getCommentsModerationService()
            throws Exception {
        CommentsModerationService commentsModerationService =
                Framework.getService(CommentsModerationService.class);
        if (commentsModerationService == null) {
            throw new WebException("Unable to get CommentsModerationService ");
        }
        return commentsModerationService;
    }

    /**
     * Can be overwritten to allow creation of localized comment. Defaults to
     * create comment in comments root.
     *
     * @param session the core session
     * @param target commented document
     * @param comment comment itself
     * @return the comment created
     */
    protected DocumentModel createCommentDocument(CoreSession session,
            DocumentModel target, DocumentModel comment) throws Exception {
        return getCommentManager().createComment(target, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to publish right away.
     *
     * @param session the core session
     * @param target commented document
     * @param comment comment itself
     */
    protected void publishComment(CoreSession session, DocumentModel target,
            DocumentModel comment) throws Exception {
        getCommentsModerationService().publishComment(session, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to delete right away.
     *
     * @param target commented document
     * @param comment comment itself
     */
    protected void deleteComment(DocumentModel target, DocumentModel comment)
            throws Exception {
        getCommentManager().deleteComment(target, comment);
    }

    /**
     * Can be overwritten to allow workflow. Defaults to reject and delete right away.
     *
     * @param target commented document
     * @param comment comment itself
     */
    protected void rejectComment(CoreSession session, DocumentModel target,
            DocumentModel comment) throws Exception {
        getCommentsModerationService().rejectComment(session, target, comment.getId());
        getCommentManager().deleteComment(target, comment);
    }


    /**
     * Can be overwritten to allow workflow. Defaults to approve right away.
     *
     * @param target commented document
     * @param comment comment itself
     */
    protected void approveComent(CoreSession session, DocumentModel target,
            DocumentModel comment) throws Exception {
        getCommentsModerationService().approveComent(session, target, comment.getId());
    }

}
