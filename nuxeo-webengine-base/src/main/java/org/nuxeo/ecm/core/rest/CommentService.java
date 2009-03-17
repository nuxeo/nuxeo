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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmService.VariableName;
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
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        try {
            // create a new webComment on this page
            DocumentModel webComment = session.createDocumentModel("WebComment");
            webComment.setPropertyValue("webcmt:author",
                    session.getPrincipal().getName());
            webComment.setPropertyValue("webcmt:text", cText);
            webComment.setPropertyValue("webcmt:creationDate", new Date());
            CommentManager commentManager = WebCommentUtils.getCommentManager();
            webComment = commentManager.createLocatedComment(
                    dobj.getDocument(), webComment, getParentWorkspacePath(
                            session, dobj.getDocument()));
            session.save();

            if (WebCommentUtils.isCurrentModerated(session, pageDoc)
                    && (!WebCommentUtils.isModeratedByCurrentUser(session,
                            pageDoc))) {
                // if current page is moderated
                // start the moderation process
                startModeration(session, pageDoc, webComment.getId());
            } else {
                // simply publish the comment
                session.followTransition(webComment.getRef(),
                        WebCommentsConstants.TRANSITION_TO_PUBLISHED_STATE);
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
        if (WebCommentUtils.isCurrentModerated(session, dobj.getDocument())
                && comment.getCurrentLifeCycleState().equals(
                        WebCommentsConstants.PENDING_STATE)) {
            JbpmService jbpmService = Framework.getService(JbpmService.class);
            TaskInstance moderationTask = getModerationTask(jbpmService,
                    session, dobj.getDocument(), docId);

            if (moderationTask == null) {
                throw new ClientException("No moderation task found");
            }

            jbpmService.endTask(moderationTask.getId(), WebCommentsConstants.PENDING_STATE,
                    null, null, null, (NuxeoPrincipal) session.getPrincipal());
        }

        CommentManager commentManager = WebCommentUtils.getCommentManager();
        commentManager.deleteComment(dobj.getDocument(), comment);
        return redirect(dobj.getPath());

    }

    /**
     * Starts the moderation on given Comment.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    protected void startModeration(CoreSession session, DocumentModel page,
            String commentID) throws Exception {

        JbpmService jbpmService = Framework.getService(JbpmService.class);
        ArrayList<String> moderators = WebCommentUtils.getModerators(session,
                page);

        if (moderators == null || moderators.isEmpty()) {
            throw new ClientException("No moderators defined");
        }

        Map<String, Serializable> vars = new HashMap<String, Serializable>();
        vars.put(VariableName.participants.name(), moderators);
        vars.put("postRef", commentID);
        jbpmService.createProcessInstance(
                (NuxeoPrincipal) session.getPrincipal(), WebCommentsConstants.MODERATION_PROCESS,
                page, vars, null);
        // Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_NEW_STARTED);

    }

    public Response rejectComment() throws Exception {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        FormData form = ctx.getForm();
        String commentId = form.getString(FormData.PROPERTY);

        JbpmService jbpmService = Framework.getService(JbpmService.class);
        TaskInstance moderationTask = getModerationTask(jbpmService, session,
                pageDoc, commentId);

        if (moderationTask == null) {
            throw new ClientException("No moderation task found");
        }

        jbpmService.endTask(moderationTask.getId(), "moderation_reject", null,
                null, null, (NuxeoPrincipal) session.getPrincipal());

        // get current comment
        DocumentModel comment = session.getDocument(new IdRef(commentId));
        // remove comment
        commentManager.deleteComment(dobj.getDocument(), comment);
        // Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_COMPLETED);
        return redirect(dobj.getPath());
    }

    
    public Response approveComent() throws Exception {
        DocumentObject dobj = (DocumentObject) getTarget();
        CoreSession session = dobj.getCoreSession();
        DocumentModel pageDoc = dobj.getDocument();
        FormData form = ctx.getForm();
        String commentId = form.getString(FormData.PROPERTY);

        JbpmService jbpmService = Framework.getService(JbpmService.class);
        TaskInstance moderationTask = getModerationTask(jbpmService, session,
                pageDoc, commentId);

        if (moderationTask == null) {
            throw new ClientException("No moderation task found");
        }
        jbpmService.endTask(moderationTask.getId(), WebCommentsConstants.TRANSITION_TO_PUBLISHED_STATE, null,
                null, null, (NuxeoPrincipal) session.getPrincipal());
        // Events.instance().raiseEvent(JbpmEventNames.WORKFLOW_TASK_COMPLETED);
        return redirect(dobj.getPath());
    }

    protected ProcessInstance getModerationProcess(JbpmService jbpmService,
            CoreSession session, DocumentModel doc, String commentId)
            throws ClientException {
        List<ProcessInstance> processes = jbpmService.getProcessInstances(doc,
                (NuxeoPrincipal) session.getPrincipal(),
                new CommentWorkflowFilter(commentId));
        if (processes != null && !processes.isEmpty()) {
            if (processes.size() > 1) {
                // log.error("There are several moderation workflows running, "
                // + "taking only first found");
            }
            return processes.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected TaskInstance getModerationTask(JbpmService jbpmService,
            CoreSession session, DocumentModel doc, String commentId)
            throws ClientException {
        ProcessInstance process = getModerationProcess(jbpmService, session,
                doc, commentId);
        if (process != null) {
            Collection tasks = process.getTaskMgmtInstance().getTaskInstances();
            if (tasks != null && !tasks.isEmpty()) {
                if (tasks.size() > 1) {
                    /*
                     * log.error("There are several moderation tasks, " +
                     * "taking only first found");
                     */
                }
                TaskInstance task = (TaskInstance) tasks.iterator().next();
                return task;
            }
        }
        return null;
    }

    public static String getParentWorkspacePath(CoreSession session,
            DocumentModel doc) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel documentModel : parents) {
            if (documentModel.getType().equals("Workspace")) {
                return documentModel.getPathAsString();
            }
        }
        return null;
    }
}
