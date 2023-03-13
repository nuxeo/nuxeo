/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.api;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * The DocumentRoutingService allows manipulation of {@link DocumentRoute DocumentRoutes}.
 */
public interface DocumentRoutingService {

    /**
     * Creates a new route instance and optionally starts it.
     * <p>
     * If {@code startInstance = false}, then the route can be started later by calling {@link #startInstance}.
     *
     * @param routeModelId the route model id
     * @param docIds the list of document bound to the instance
     * @param map the values to pass as initial workflow variables
     * @param session the session
     * @param startInstance if the route is automatically started
     * @return the created route instance id
     */
    String createNewInstance(String routeModelId, List<String> docIds, Map<String, Serializable> map,
            CoreSession session, boolean startInstance);

    /**
     * Creates a new route instance and optionally starts it.
     * <p>
     * If {@code startInstance = false}, then the route can be started later by calling {@link #startInstance}.
     *
     * @param routeModelId the route model id
     * @param docIds The list of document bound to the instance.
     * @param session the session
     * @param startInstance if the route is automatically started
     * @return the created route instance id
     */
    String createNewInstance(String routeModelId, List<String> docIds, CoreSession session, boolean startInstance);

    /**
     * Create a new {@link DocumentRoute} instance from this {@link DocumentRoute} model.
     *
     * @param model The model used to create the instance.
     * @param documentIds The list of document bound to the instance.
     * @param startInstance if the {@link DocumentRoute} is automatically started.
     * @return the created {@link DocumentRoute} instance.
     */
    DocumentRoute createNewInstance(DocumentRoute model, List<String> documentIds, CoreSession session,
            boolean startInstance);

    /**
     * @deprecated since 5.6, use other APIs
     */
    @Deprecated
    DocumentRoute createNewInstance(DocumentRoute model, String documentId, CoreSession session, boolean startInstance);

    /**
     * @deprecated since 5.6, use other APIs
     */
    @Deprecated
    DocumentRoute createNewInstance(DocumentRoute model, List<String> documentIds, CoreSession session);

    /**
     * @deprecated since 5.6, use other APIs
     */
    @Deprecated
    DocumentRoute createNewInstance(DocumentRoute model, String documentId, CoreSession session);

    /**
     * Starts an instance that was created with {@link #createNewInstance} but with {@code startInstance = false}.
     *
     * @param routeInstanceId the route instance id
     * @param docIds the list of document bound to the instance
     * @param map the values to pass as initial workflow variables
     * @param session the session
     * @since 5.7.2
     */
    void startInstance(String routeInstanceId, List<String> docIds, Map<String, Serializable> map, CoreSession session);

    /**
     * Resumes a route instance on a give node. Any remaining tasks on this node will be cancelled.
     * <p/>
     * Called by the UI action corresponding to a task button.
     * <p/>
     * If all attached documents of the workflow instance have been deleted then the workflow is cancelled.
     *
     * @param routeId the id of the route instance
     * @param nodeId the node id to resume on
     * @param data the data coming from UI form
     * @param status the status coming from UI form
     * @param session the session
     * @since 5.6
     */
    void resumeInstance(String routeId, String nodeId, Map<String, Object> data, String status, CoreSession session);

    /**
     * Completes a task on a give node. If this is the last task the workflow will continue.
     * <p/>
     * Called by the UI action corresponding to a task button.
     *
     * @param routeId the id of the route instance
     * @param taskId the id of the task
     * @param data the data coming from UI form
     * @param status the status coming from UI form
     * @param session the session
     * @since 5.6
     */
    void completeTask(String routeId, String taskId, Map<String, Object> data, String status, CoreSession session);

    /**
     * Save a route instance as a new model of route.
     * <p/>
     * The place in which the new instance is persisted and its name depends on {@link DocumentRoutingPersister}. The
     * route instance should be in either running, done or ready state. The new route model will be in draft state and
     * won't have any attached documents.
     *
     * @param route the instance from which we create a new model.
     * @return the new model in draft state.
     */
    DocumentRoute saveRouteAsNewModel(DocumentRoute route, CoreSession session);

    /**
     * Return the list of available {@link DocumentRoute} model the user can start.
     *
     * @param session The session of the user.
     * @return A list of available {@link DocumentRoute}
     */
    List<DocumentRoute> getAvailableDocumentRouteModel(CoreSession session);

    /**
     * Return the list of available {@link DocumentRoute} document route.
     *
     * @param session The session of the user.
     * @return A list of available {@link DocumentRoute}
     * @since 7.2
     */
    List<DocumentRoute> getAvailableDocumentRoute(CoreSession session);

    /**
     * Return the operation chain to run for a documentType. The document type should extend the DocumentRouteStep. Use
     * the <code>chainsToType</code> extension point to contribute new mapping.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     * @param documentType The document type
     * @return The operation chain id.
     */
    @Deprecated
    String getOperationChainId(String documentType);

    /**
     * Return the operation chain to undo a step when the step is in running state. The document type should extend the
     * DocumentRouteStep. Use the <code>chainsToType</code> extension point to contribute new mapping.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     * @param documentType
     * @return
     */
    @Deprecated
    String getUndoFromRunningOperationChainId(String documentType);

    /**
     * Return the operation chain to undo a step when the step is in done state. The document type should extend the
     * DocumentRouteStep. Use the <code>chainsToType</code> extension point to contribute new mapping.
     *
     * @param documentType
     * @return
     */
    String getUndoFromDoneOperationChainId(String documentType);

    /**
     * Validates the given {@link DocumentRoute} model by changing its lifecycle state and setting it and all its
     * children in ReadOnly.
     *
     * @return The validated route.
     */
    DocumentRoute validateRouteModel(DocumentRoute routeModel, CoreSession session)
            throws DocumentRouteNotLockedException;

    /**
     * Unlock the given {@link DocumentRoute} model under unrestricted session.
     *
     * @param routeModel
     * @param userSession
     * @return The unlocked route.
     * @since 1.9
     */
    public DocumentRoute unlockDocumentRouteUnrestrictedSession(final DocumentRoute routeModel, CoreSession userSession);

    /**
     * Computes the list of elements {@link DocumentRouteTableElement} for this {@link DocumentRoute}.
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session The session used to query the {@link DocumentRoute}.
     * @param A list of {@link DocumentRouteElement}
     */
    List<DocumentRouteTableElement> getRouteElements(DocumentRoute route, CoreSession session);

    /**
     * Return the list of related {@link DocumentRoute} in a state for a given attached document.
     *
     * @param session The session used to query the {@link DocumentRoute}.
     * @param states the list of states.
     * @return A list of available {@link DocumentRoute}
     */
    List<DocumentRoute> getDocumentRoutesForAttachedDocument(CoreSession session, String attachedDocId,
            List<DocumentRouteElement.ElementLifeCycleState> states);

    /**
     * @param session
     * @param attachedDocId
     * @return
     * @see #getDocumentRoutesForAttachedDocument(CoreSession, String, List) for route running or ready.
     */
    List<DocumentRoute> getDocumentRoutesForAttachedDocument(CoreSession session, String attachedDocId);

    /**
     * if the user can validate a route.
     *
     * @deprecated use {@link #canValidateRoute(DocumentModel, CoreSession)} instead.
     */
    @Deprecated
    boolean canUserValidateRoute(NuxeoPrincipal currentUser);

    /**
     * Checks if the principal that created the client session can validate the route
     *
     * @param documentRoute
     * @param coreSession
     */
    boolean canValidateRoute(DocumentModel documentRoute, CoreSession coreSession);

    /**
     * Add a route element in another route element.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     * @param parentDocumentRef The DocumentRef of the parent document.
     * @param idx The position of the document in its container.
     * @param routeElement The document to add.
     * @param session
     */
    @Deprecated
    void addRouteElementToRoute(DocumentRef parentDocumentRef, int idx, DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException;

    /**
     * Add a route element in another route element.
     * <p/>
     * If the parent element is in draft state, the routeElement is kept in draft state. Otherwise, the element is set
     * to 'ready' state.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     * @param parentDocumentRef The DocumentRef of the parent document.
     * @param sourceName the name of the previous document in the container.
     * @param routeElement the document to add.
     * @param session
     */
    @Deprecated
    void addRouteElementToRoute(DocumentRef parentDocumentRef, String sourceName, DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException;

    /**
     * Remove the given route element
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     * @param The route element document.
     * @param session
     */
    @Deprecated
    void removeRouteElement(DocumentRouteElement routeElement, CoreSession session)
            throws DocumentRouteNotLockedException;

    /**
     * Get the children of the given stepFolder ordered by the ecm:pos metadata.
     *
     * @deprecated since 5.9.2 - Use only routes of type 'graph'
     * @param stepFolderId
     * @param session
     * @return
     */
    @Deprecated
    DocumentModelList getOrderedRouteElement(String routeElementId, CoreSession session);

    /**
     * Locks this {@link DocumentRoute} if not already locked by the current user. If the document is already locked by
     * another user and {@link DocumentRouteAlredayLockedException} is thrown
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session The session used to lock the {@link DocumentRoute}.
     * @throws {@link DocumentRouteAlredayLockedException}
     */
    void lockDocumentRoute(DocumentRoute routeModel, CoreSession session) throws DocumentRouteAlredayLockedException;

    /**
     * Unlocks this {@link DocumentRoute}.If the document is not locked throws a {@link DocumentRouteNotLockedException}
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session The session used to lock the {@link DocumentRoute}.
     */
    void unlockDocumentRoute(DocumentRoute routeModel, CoreSession session) throws DocumentRouteNotLockedException;

    /**
     * Update the given route element
     *
     * @param The route element document.
     * @param session
     */
    void updateRouteElement(DocumentRouteElement routeModel, CoreSession session)
            throws DocumentRouteNotLockedException;

    /**
     * Verify is this {@link DocumentRoute} is already locked by the current user.
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session
     */
    boolean isLockedByCurrentUser(DocumentRoute routeModel, CoreSession session);

    /**
     * Checks if the given document can be associated to a DocumentRoute.
     *
     * @param doc the document
     * @return {@code true} if the document can be routed
     */
    boolean isRoutable(DocumentModel doc);

    /**
     * Imports all the route models resource templates.
     *
     * @param session the core session to use
     * @since 7.3
     */
    void importAllRouteModels(CoreSession session);

    /**
     * Creates a route model in the root models folder defined by the current persister. The templateResource is a zip
     * tree xml export of a route document and it is imported using the core-io importer.
     *
     * @param templateResource
     * @param overwrite
     * @param session
     * @since 5.6
     */
    DocumentRoute importRouteModel(URL templateResource, boolean overwrite, CoreSession session);

    /**
     * Registers a new route model template to be imported at application startup.
     *
     * @param resource the resource
     * @since 5.6
     */
    void registerRouteResource(RouteModelResourceType resource, RuntimeContext extensionContext);

    /**
     * Returns all the route models resource templates. Use the <code>routeModelImporter</code> extension point to
     * contribute new resources.
     *
     * @since 5.6
     */
    List<URL> getRouteModelTemplateResources();

    /**
     * Returns the route models matching the {@code searchString}.
     *
     * @since 5.6
     */
    List<DocumentModel> searchRouteModels(CoreSession session, String searchString);

    /**
     * Returns the route model with the given id
     *
     * @since 5.6
     */
    DocumentRoute getRouteModelWithId(CoreSession session, String id);

    // copied from the deprecated RoutingTaskService

    /**
     * Returns the doc id of the route model with the given id
     *
     * @since 5.7
     */
    String getRouteModelDocIdWithId(CoreSession session, String id);

    /**
     * Marks the tasks as Routing tasks.
     * <p>
     * This allows the related documents to be adapted to {@link RoutingTask}.
     *
     * @param session the session
     * @param tasks the tasks
     * @since 5.6, was on RoutingTaskService before
     * @deprecated The facet RoutingTask is statically attached to the new RoutingTask Document type since 7.1
     */
    @Deprecated
    void makeRoutingTasks(CoreSession session, List<Task> tasks);

    /**
     * Ends a task. If this is the last task the workflow will continue.
     *
     * @param session
     * @param task
     * @param data
     * @param status name of the button clicked to submit the task form
     * @since 5.6, was on RoutingTaskService before
     */
    void endTask(CoreSession session, Task task, Map<String, Object> data, String status);

    /**
     * Grants on these documents the specified assignees permissions for this task.
     *
     * @param session the session
     * @param permission the permission
     * @param docs the documents
     * @param task the task
     * @since 5.6
     */
    void grantPermissionToTaskAssignees(CoreSession session, String permission, List<DocumentModel> docs, Task task);

    /**
     * Removes on these documents the specified assignees permissions for this task.
     *
     * @param session the session
     * @param docs the documents
     * @param task the task
     * @since 5.6
     */
    void removePermissionFromTaskAssignees(CoreSession session, List<DocumentModel> docs, Task task);

    /**
     * Gets the documents following the workflow to which the given task belongs
     *
     * @param session
     * @param task
     * @return
     * @since 5.6, was on RoutingTaskService before
     */
    List<DocumentModel> getWorkflowInputDocuments(CoreSession session, Task task);

    /**
     * Finishes an open task. All permissions granted to the tasks assignees on the document following the worklflow are
     * removed. Doesn't resume the workflow as the <code>completeTask</code> method. Not executed using an unrestricted
     * session.
     *
     * @param session
     * @param route
     * @param task
     * @param delete
     * @throws DocumentRouteException
     * @since 5.7
     * @deprecated // will be removed in 5.8, use completeTask instead
     */
    void finishTask(CoreSession session, DocumentRoute route, Task task, boolean delete) throws DocumentRouteException;

    /**
     * Cancels an open task. If the task was created by an workflow, all permissions granted to the tasks assignees on
     * the document following the worklflow are removed. Doesn't resume the workflow as the <code>completeTask</code>
     * method.
     *
     * @param session
     * @param task
     * @param delete
     * @throws DocumentRouteException
     * @since 5.7.3
     */
    void cancelTask(CoreSession session, String taskId) throws DocumentRouteException;

    /**
     * Reassigns the given task to the list of actors. Removes the permissions granted on the document following the
     * workflow to the current task assignees and grants them to the new actors.
     *
     * @param session
     * @param taskId
     * @param actors
     * @param comment
     * @since 5.7.3
     */
    void reassignTask(CoreSession session, String taskId, List<String> actors, String comment)
            throws DocumentRouteException;

    /**
     * Reassigns the given task to the list of actors. Grants to new delegated actors the same permissions as the task
     * assignees on the document following the workflow .
     *
     * @param session
     * @param taskId
     * @param delegatedActors
     * @param comment
     * @since 5.8
     */
    void delegateTask(CoreSession session, String taskId, List<String> delegatedActors, String comment)
            throws DocumentRouteException;

    /**
     * Grants on these documents the specified assignees permissions for this task to the tasks delegated actors.
     *
     * @param session the session
     * @param permission the permission
     * @param docs the documents
     * @param task the task
     * @since 5.8
     */
    void grantPermissionToTaskDelegatedActors(CoreSession session, String permission, List<DocumentModel> docs,
            Task task);

    /**
     * Removes on these documents the specified assignees permissions for the task actors and also tasks delegated
     * actors if this task was delegated
     *
     * @param session the session
     * @param docs the documents
     * @param task the task
     * @since 5.8
     */
    void removePermissionsForTaskActors(CoreSession session, List<DocumentModel> docs, Task task);

    /**
     * Removes on these documents the specified assignees permissions for the task actors and also tasks delegated
     * actors if this task was delegated
     *
     * @param session the session
     * @param docs the documents
     * @param taskId the taskId
     * @since 7.4
     */
    void removePermissionsForTaskActors(CoreSession session, List<DocumentModel> docs, String taskId);

    /**
     * Query for the routes 'done' or 'canceled' and delete them. The max no of the routes that will be deleted is
     * specified by the 'limit' parameter. When the limit is '0' all the completed routes are deleted.
     *
     * @since 5.8
     * @deprecated since 2023, use #{@link #cleanupRouteInstances(String)} instead.
     */
    @Deprecated
    void cleanupDoneAndCanceledRouteInstances(String repositoryName, int limit);

    /**
     * Remove the routes in state 'done' or 'canceled' of the given repository.
     * <p>
     * If 'nuxeo.routing.cleanup.workflow.instances.orphan' framework property is set to true, orphan routes (for which
     * all associated documents have been removed) will also be removed.
     *
     * @param repositoryName the repository name to clean up
     * @since 2023
     */
    void cleanupRouteInstances(String repositoryName);

    /**
     * @since 5.9.3
     */
    void invalidateRouteModelsCache();

    /**
     * Query for the routes 'done' or 'canceled' and delete them. The max no of the routes that will be deleted is
     * specified by the 'limit' parameter. When the limit is '0' all the completed routes are deleted. The routes to be
     * deleted are ordered ascending by their creation date.
     *
     * @return always -1
     * @since 7.1
     * @deprecated since 2023, use GarbageCollectOrphanRoutesAction instead
     */
    @Deprecated
    int doCleanupDoneAndCanceledRouteInstances(String reprositoryName, int limit);

    /**
     * @param userId
     * @param workflowInstanceId
     * @param session
     * @return
     * @since 7.2
     * @deprecated since 11.1 this method does not scale as it does not paginate results. Use
     *             {@link org.nuxeo.ecm.platform.routing.core.provider.RoutingTaskPageProvider#getCurrentPage()}
     *             instead.
     */
    @Deprecated
    List<Task> getTasks(final DocumentModel document, String actorId, String workflowInstanceId,
            String workflowModelName, CoreSession session);

    /**
     * @param document
     * @param session
     * @return
     * @since 7.2
     */
    List<DocumentRoute> getDocumentRelatedWorkflows(final DocumentModel document, final CoreSession session);

    /**
     * @since 7.2
     */
    List<DocumentRoute> getRunningWorkflowInstancesLaunchedByCurrentUser(final CoreSession session);

    /**
     * @since 7.2
     */
    List<DocumentRoute> getRunningWorkflowInstancesLaunchedByCurrentUser(CoreSession session, String worflowModelName);

    /**
     * Returns true if the document route is a model, false if it is just an instance i.e. a running workflow.
     *
     * @since 7.2
     */
    boolean isWorkflowModel(final DocumentRoute documentRoute);


    /**
     * Remove the workflow instance if it is canceled, done or orphan. An orphan instance has all its attached document
     * already removed.
     *
     * @return true if the route has been deleted, false otherwise
     * @since 2023
     */
    boolean purgeDocumentRoute(CoreSession session, DocumentRoute route);

}
