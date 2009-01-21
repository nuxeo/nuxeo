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
 *     arussel
 */
package org.nuxeo.ecm.platform.publishing.api;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author arussel
 *
 */
public interface Publisher {

    void submitToPublication(DocumentModel document,
            DocumentModel placeToPublishTo, NuxeoPrincipal principal)
            throws PublishingException;

    boolean hasValidationTask(DocumentModel proxy, NuxeoPrincipal currentUser)
            throws PublishingException;

    void validatorPublishDocument(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException;

    void validatorRejectPublication(DocumentModel doc,
            NuxeoPrincipal principal, String comment) throws PublishingException;

    /*
     * // Handle mandatory comment here since we use several commandLinks for //
     * now within the same form sharing the same comment textarea. // Therefore,
     * we can simply use the jsf control... // Of course it remains a temporary
     * solution. if (rejectPublishingComment == null ||
     * rejectPublishingComment.trim().length() <= 0) {
     * facesMessages.addToControl("rejectPublishingComment",
     * FacesMessage.SEVERITY_ERROR, resourcesAccessor.getMessages().get(
     * "label.publishing.reject.user.comment.mandatory")); return null; }
     *
     * // Compute parent before deleting the document. DocumentModel
     * currentDocument = getCurrentDocument(); DocumentModel parent; try {
     * parent = documentManager.getDocument(currentDocument.getParentRef()); }
     * catch (ClientException ce) { throw new PublishingException(ce); }
     *
     * WMParticipant creator; try { PublishingTasks tasks = new PublishingTasks(
     * navigationContext.getCurrentDocument(), currentUser); WMWorkItemInstance
     * wi = tasks.getPublishingWorkItem(); if (wi == null) { throw new
     * PublishingException( "No publishing task found for user=" +
     * currentUser.getName()); } creator = wi.getProcessInstance().getAuthor();
     * wapi.endWorkItem(wi.getId(),
     * PublishingConstants.WORKFLOW_TRANSITION_TO_REJECT); } catch
     * (WMWorkflowException e) { throw new PublishingException(e); }
     *
     * // Notify reject event DocumentModel sourceDocument; try { CoreSession
     * session; LoginContext context; Repository repository; try { context =
     * Framework.login(); RepositoryManager repositoryMgr =
     * Framework.getService(RepositoryManager.class); repository =
     * repositoryMgr.getRepository(currentDocument.getRepositoryName()); session
     * = repository.open(); } catch (Exception e) { throw new
     * ClientException(e); }
     *
     * String proxySourceId = session.getDocument( new
     * IdRef(currentDocument.getSourceId())).getSourceId(); sourceDocument =
     * session.getDocument(new IdRef(proxySourceId)); try { if (repository !=
     * null && session != null) { repository.close(session); } if (context !=
     * null) { context.logout(); } } catch (Exception e) { throw new
     * ClientException(e); }
     *
     * String section = documentManager.getDocument(
     * getCurrentDocument().getParentRef()).getName();
     *
     * Map<String, Serializable> eventInfo = new HashMap<String,
     * Serializable>(); eventInfo.put("targetSection", section);
     * eventInfo.put("proxy", currentDocument);
     * eventInfo.put(WorkflowConstants.WORKFLOW_CREATOR, creator.getName());
     *
     * notifyEvent(
     * org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_PUBLICATION_REJECTED,
     * eventInfo, rejectPublishingComment, sourceDocument); } catch
     * (ClientException e) { throw new PublishingException(e); }
     *
     * // Here to invalidate the dashboard items.
     * Events.instance().raiseEvent(EventNames.WORKFLOW_TASK_STOP);
     *
     * // Redirect to the parent since the workflow is expected to delete the //
     * document that has been submited.
     *
     * navigationContext.resetCurrentContext(); Events.instance().raiseEvent(
     * org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_SELECTION_CHANGED);
     *
     * String view = null; try { view =
     * navigationContext.navigateToDocument(parent); } catch (ClientException e)
     * { log.error("An error occured while redirecting."); }
     *
     * rejectPublishingComment = null;
     *
     * facesMessages.add(FacesMessage.SEVERITY_INFO,
     * resourcesAccessor.getMessages().get("document_rejected"),
     * resourcesAccessor.getMessages().get(sourceDocument.getType()));
     *
     * return view;
     */

    boolean canManagePublishing(DocumentModel currentDocument,
            NuxeoPrincipal currentUser) throws PublishingException;

    /**
     * check if this proxy is published
     *
     * @param proxy
     * @return
     */
    boolean isPublished(DocumentModel proxy) throws PublishingException;
}
