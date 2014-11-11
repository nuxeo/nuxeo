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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentUtils;
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentsConstants;
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
 */
@WebAdapter(name = "comments", type = "CommentService", targetType = "Document", targetFacets = { "Commentable" })
public class CommentService extends DefaultAdapter {

    @POST
    public Response doPost(@FormParam("text") String cText) {
        if (cText == null) {
            throw new IllegalParameterException("Expecting a 'text' parameter");
        }
        DocumentObject dobj = (DocumentObject) getTarget();
        CommentManager commentManager = getCommentManager();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        try {

            DocumentModel webComment = session.createDocumentModel("Comment");
            webComment.setPropertyValue("comment:author",
                    session.getPrincipal().getName());
            webComment.setPropertyValue("comment:text", cText);
            webComment.setPropertyValue("comment:creationDate", new Date());
            webComment = commentManager.createLocatedComment(
                    dobj.getDocument(), webComment, getParentWorkspacePath(
                            session, dobj.getDocument()));
            session.save();
            CommentsModerationService commentsModerationService = getCommentsModerationService();
            if (WebCommentUtils.isCurrentModerated(session, pageDoc)
                    && (!WebCommentUtils.isModeratedByCurrentUser(session,
                            pageDoc))){
                // if current page is moderated
                // get all moderators
                ArrayList<String> moderators = WebCommentUtils.getModerators(
                        session, pageDoc);
                // start the moderation process
                commentsModerationService.startModeration(session, pageDoc,
                        webComment.getId(), moderators);
            } else {
                // simply publish the comment
                commentsModerationService.publishComment(session, webComment);
            }

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
            return rejectComment();
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
            return approveComent();
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
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        commentManager.deleteComment(dobj.getDocument(), comment);
        return redirect(dobj.getPath());
    }


    public Response rejectComment() throws Exception {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        FormData form = ctx.getForm();
        String commentId = form.getString(FormData.PROPERTY);
        
        CommentsModerationService commentsModerationService = getCommentsModerationService();
        commentsModerationService.rejectComment(session, pageDoc, commentId);
   
        // get current comment
        DocumentModel comment = session.getDocument(new IdRef(commentId));
        // remove comment
        commentManager.deleteComment(dobj.getDocument(), comment);
        return redirect(dobj.getPath());
    }

    
    public Response approveComent() throws Exception {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        FormData form = ctx.getForm();
        String commentId = form.getString(FormData.PROPERTY);
        
        CommentsModerationService commentsModerationService = getCommentsModerationService();
        commentsModerationService.approveComent(session, pageDoc, commentId);
        
        return redirect(dobj.getPath());
    }


    public static CommentsModerationService getCommentsModerationService()
            throws Exception {
        CommentsModerationService commentsModerationService = Framework.getService(CommentsModerationService.class);
        if (commentsModerationService == null) {
            throw new Exception("Unable to get CommentsModerationService ");
        }
        return commentsModerationService;
    }
    
    public static String getParentWorkspacePath(CoreSession session,
            DocumentModel doc) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        Collections.reverse(parents);
        for (DocumentModel currentDocumentModel : parents) {
            if ("Workspace".equals(currentDocumentModel.getType())
                    && currentDocumentModel.hasFacet("WebView")) {
                return currentDocumentModel.getPathAsString();
            }
        }
        return null;
    }
    
    protected static CommentManager getCommentManager() {
        return Framework.getLocalService(CommentManager.class);
    }

}
