/*
 * (C) Copyright 2009-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.routing.api;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteAlredayLockedException;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteNotLockedException;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * The DocumentRoutingService allows manipulation of {@link DocumentRoute
 * DocumentRoutes}.
 */
public interface DocumentRoutingService {

    /**
     * Create a new {@link DocumentRoute} instance from this
     * {@link DocumentRoute} model.
     *
     * @param model The model used to create the instance.
     * @param documents The list of document bound to the instance.
     * @param startInstance if the {@link DocumentRoute} is automatically
     *            started.
     * @return the created {@link DocumentRoute} instance.
     */
    DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session, boolean startInstance);

    /**
     * @see #createNewInstance(DocumentRoute, List, CoreSession, boolean) with
     *      only one document attached.
     */
    DocumentRoute createNewInstance(DocumentRoute model, String documentId,
            CoreSession session, boolean startInstance);

    /**
     * @see #createNewInstance(DocumentRoute, List, CoreSession, boolean) with
     *      startInstance <code>true</code>
     */
    DocumentRoute createNewInstance(DocumentRoute model,
            List<String> documentIds, CoreSession session);

    /**
     * @see #createNewInstance(DocumentRoute, List, CoreSession, boolean) with
     *      startInstance <code>true</code> and only one document attached.
     */
    DocumentRoute createNewInstance(DocumentRoute model, String documentId,
            CoreSession session);

    /**
     * Resumes a route instance.
     * <p/>
     * Called by the UI action corresponding to a task button.
     *
     * @param routeRef the reference to the route instance document
     * @param session the session
     * @param nodeId the node id to resume on
     * @param data the data coming from UI form
     * @since 5.6
     */
    void resumeInstance(DocumentRef routeRef, CoreSession session,
            String nodeId, Map<String, Object> data);

    /**
     * Save a route instance as a new model of route.
     * <p/>
     * The place in which the new instance is persisted and its name depends on
     * {@link DocumentRoutingPersister}. The route instance should be in either
     * running, done or ready state. The new route model will be in draft state
     * and won't have any attached documents.
     *
     * @param route the instance from which we create a new model.
     * @return the new model in draft state.
     */
    DocumentRoute saveRouteAsNewModel(DocumentRoute route, CoreSession session);

    /**
     * Return the list of available {@link DocumentRoute} model the user can
     * start.
     *
     * @param session The session of the user.
     * @return A list of available {@link DocumentRoute}
     */
    List<DocumentRoute> getAvailableDocumentRouteModel(CoreSession session);

    /**
     * Return the operation chain to run for a documentType. The document type
     * should extend the DocumentRouteStep. Use the <code>chainsToType</code>
     * extension point to contribute new mapping.
     *
     * @param documentType The document type
     * @return The operation chain id.
     */
    String getOperationChainId(String documentType);

    /**
     * Return the operation chain to undo a step when the step is in running
     * state. The document type should extend the DocumentRouteStep. Use the
     * <code>chainsToType</code> extension point to contribute new mapping.
     *
     * @param documentType
     * @return
     */
    String getUndoFromRunningOperationChainId(String documentType);

    /**
     * Return the operation chain to undo a step when the step is in done state.
     * The document type should extend the DocumentRouteStep. Use the
     * <code>chainsToType</code> extension point to contribute new mapping.
     *
     * @param documentType
     * @return
     */
    String getUndoFromDoneOperationChainId(String documentType);

    /**
     * Validates the given {@link DocumentRoute} model by changing its lifecycle
     * state and setting it and all its children in ReadOnly.
     *
     * @return The validated route.
     */
    DocumentRoute validateRouteModel(DocumentRoute routeModel,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException;

    /**
     * Unlock the given {@link DocumentRoute} model under unrestricted session.
     *
     * @param routeModel
     * @param userSession
     * @return The unlocked route.
     * @since 1.9
     */
    public DocumentRoute unlockDocumentRouteUnrestrictedSession(
            final DocumentRoute routeModel, CoreSession userSession)
            throws ClientException;

    /**
     * Computes the list of elements {@link DocumentRouteTableElement} for this
     * {@link DocumentRoute}.
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session The session used to query the {@link DocumentRoute}.
     * @param A list of {@link DocumentRouteElement}
     */
    List<DocumentRouteTableElement> getRouteElements(DocumentRoute route,
            CoreSession session);

    /**
     * Return the list of related {@link DocumentRoute} in a state for a given
     * attached document.
     *
     * @param session The session used to query the {@link DocumentRoute}.
     * @param states the list of states.
     * @return A list of available {@link DocumentRoute}
     */
    List<DocumentRoute> getDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId,
            List<DocumentRouteElement.ElementLifeCycleState> states);

    /**
     * @param session
     * @param attachedDocId
     * @return
     * @see #getDocumentRoutesForAttachedDocument(CoreSession, String, List) for
     *      route running or ready.
     */
    List<DocumentRoute> getDocumentRoutesForAttachedDocument(
            CoreSession session, String attachedDocId);

    /**
     * if the user can validate a route.
     *
     * @deprecated use {@link #canValidateRoute(DocumentModel, CoreSession)}
     *             instead.
     */
    @Deprecated
    boolean canUserValidateRoute(NuxeoPrincipal currentUser);

    /**
     * Checks if the principal that created the client session can validate the
     * route
     *
     * @param documentRoute
     * @param coreSession
     * @throws ClientException
     */
    boolean canValidateRoute(DocumentModel documentRoute,
            CoreSession coreSession) throws ClientException;

    /**
     * Add a route element in another route element.
     *
     * @param parentDocumentRef The DocumentRef of the parent document.
     * @param idx The position of the document in its container.
     * @param routeElement The document to add.
     * @param session
     * @throws ClientException
     */
    void addRouteElementToRoute(DocumentRef parentDocumentRef, int idx,
            DocumentRouteElement routeElement, CoreSession session)
            throws DocumentRouteNotLockedException, ClientException;

    /**
     * Add a route element in another route element.
     * <p/>
     * If the parent element is in draft state, the routeElement is kept in
     * draft state. Otherwise, the element is set to 'ready' state.
     *
     * @param parentDocumentRef The DocumentRef of the parent document.
     * @param sourceName the name of the previous document in the container.
     * @param routeElement the document to add.
     * @param session
     * @throws ClientException
     */
    void addRouteElementToRoute(DocumentRef parentDocumentRef,
            String sourceName, DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException;

    /**
     * Remove the given route element
     *
     * @param The route element document.
     * @param session
     * @throws ClientException
     */
    void removeRouteElement(DocumentRouteElement routeElement,
            CoreSession session) throws DocumentRouteNotLockedException,
            ClientException;

    /**
     * Get the children of the given stepFolder ordered by the ecm:pos metadata.
     *
     * @param stepFolderId
     * @param session
     * @return
     * @throws ClientException
     */
    DocumentModelList getOrderedRouteElement(String routeElementId,
            CoreSession session) throws ClientException;

    /**
     * Locks this {@link DocumentRoute} if not already locked by the current
     * user. If the document is already locked by another user and
     * {@link DocumentRouteAlredayLockedException} is thrown
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session The session used to lock the {@link DocumentRoute}.
     * @throws ClientException
     * @throws {@link DocumentRouteAlredayLockedException}
     */
    void lockDocumentRoute(DocumentRoute routeModel, CoreSession session)
            throws DocumentRouteAlredayLockedException, ClientException;

    /**
     * Unlocks this {@link DocumentRoute}.If the document is not locked throws a
     * {@link DocumentRouteNotLockedException}
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session The session used to lock the {@link DocumentRoute}.
     * @throws {@link ClientException}
     */
    void unlockDocumentRoute(DocumentRoute routeModel, CoreSession session)
            throws DocumentRouteNotLockedException, ClientException;

    /**
     * Update the given route element
     *
     * @param The route element document.
     * @param session
     * @throws ClientException
     */
    void updateRouteElement(DocumentRouteElement routeModel, CoreSession session)
            throws DocumentRouteNotLockedException, ClientException;

    /**
     * Verify is this {@link DocumentRoute} is already locked by the current
     * user.
     *
     * @param routeDocument {@link DocumentRoute}.
     * @param session
     * @throws ClientException
     */
    boolean isLockedByCurrentUser(DocumentRoute routeModel, CoreSession session)
            throws ClientException;

    /**
     * Checks if the given document can be associated to a DocumentRoute.
     *
     * @param doc the document
     * @return {@code true} if the document can be routed
     */
    boolean isRoutable(DocumentModel doc);

    /**
     * Creates a route model in the root models folder defined by the current
     * persister. The templateResource is a zip tree xml export of a route
     * document and it is imported using the core-io importer.
     *
     * @param templateResource
     * @param overwrite
     * @param session
     * @throws ClientException
     * @since 5.6
     */
    DocumentRoute importRouteModel(URL templateResource, boolean overwrite,
            CoreSession session) throws ClientException;

    /**
     * Registers a new route model template to be imported at application
     * startup.
     *
     * @param resource the resource
     * @since 5.6
     */
    void registerRouteResource(RouteModelResourceType resource,
            RuntimeContext extensionContext);

    /**
     * Returns all the route models resource templates. Use the
     * <code>routeModelImporter</code> extension point to contribute new
     * resources.
     *
     * @since 5.6
     */
    List<URL> getRouteModelTemplateResources() throws ClientException;

    /**
     * Returns the route models matching the {@code searchString}.
     *
     * @since 5.6
     */
    List<DocumentModel> searchRouteModels(CoreSession session,
            String searchString) throws ClientException;

}
