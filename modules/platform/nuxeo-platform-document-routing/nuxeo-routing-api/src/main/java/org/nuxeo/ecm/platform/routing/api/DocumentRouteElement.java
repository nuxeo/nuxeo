/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.lifecycle.event.BulkLifeCycleChangeListener;

/**
 * An element of a {@link DocumentRoute}
 *
 * @author arussel
 */
public interface DocumentRouteElement extends Serializable {

    /**
     * The lifecycle state of an element
     */
    enum ElementLifeCycleState {
        draft, validated, ready, running, done, canceled
    }

    /**
     * The transition of the lifecycle state.
     */
    enum ElementLifeCycleTransistion {
        toValidated, toReady, toRunning, toDone, backToReady, toCanceled, toDraft
    }

    /**
     * Return the list of documents that this route processes.
     *
     * @param session the session used to fetch the documents
     */
    DocumentModelList getAttachedDocuments(CoreSession session);

    /**
     * Return the DocumentRoute this element is part of.
     *
     * @param session The session use to fetch the route.
     */
    DocumentRoute getDocumentRoute(CoreSession session);

    /**
     * if the route this element is part of has been validated.
     */
    boolean isValidated();

    /**
     * if this element is ready.
     */
    boolean isReady();

    /**
     * if this route is done.
     */
    boolean isDone();

    /**
     * if this route is running.
     */
    boolean isRunning();

    /**
     * if this route is draft.
     */
    boolean isDraft();

    /**
     * The name of this element.
     */
    String getName();

    /**
     * the description of this element.
     */
    String getDescription();

    /**
     * Execute this element. If this is a step, it will run the operation, if this is a containter it will run its
     * children.
     */
    void run(CoreSession session);

    /**
     * Execute this element. If this is a step, it will run the operation, if this is a container it will run its
     * children.
     *
     * @param map the values to pass as initial workflow variables
     */
    void run(CoreSession session, Map<String, Serializable> map);

    /**
     * Resumes execution on a route node.
     *
     * @param session the session
     * @param nodeId the node id to resume on
     * @param taskId the task id
     * @param data the data coming from UI form
     * @param status the id of the button clicked to submit the related task form
     * @since 5.6
     */
    void resume(CoreSession session, String nodeId, String taskId, Map<String, Object> data, String status);

    /**
     * Set this element to the validate state and put it in read only mode.
     */
    void validate(CoreSession session);

    /**
     * Get the underlying document representing this element.
     */
    DocumentModel getDocument();

    /**
     * save the document representing this DocumentRoute.
     */
    void save(CoreSession session);

    /**
     * set this element as validated.
     */
    void setValidated(CoreSession session);

    /**
     * set this element as ready.
     */
    void setReady(CoreSession session);

    /**
     * set this element as running.
     */
    void setRunning(CoreSession session);

    /**
     * set this element as done.
     */
    void setDone(CoreSession session);

    /**
     * remove write rights to everyone but the administrators.
     */
    void setReadOnly(CoreSession session);

    /**
     * make this element follow a transition.
     *
     * @param transition the followed transition.
     * @param session the session used to follow the transition.
     * @param recursive If this element has children, do we recurse the follow transition.
     * @see BulkLifeCycleChangeListener
     */
    void followTransition(ElementLifeCycleTransistion transition, CoreSession session, boolean recursive);

    /**
     * If this session can validate the step.
     */
    boolean canValidateStep(CoreSession session);

    /**
     * make this user or group a validator for this step.
     */
    void setCanValidateStep(CoreSession session, String userOrGroup);

    /**
     * If this session can update this step.
     */
    boolean canUpdateStep(CoreSession session);

    /**
     * make this user or group a step updater.
     */
    void setCanUpdateStep(CoreSession session, String userOrGroup);

    /**
     * make this user or group a step reader.
     */
    void setCanReadStep(CoreSession session, String userOrGroup);

    /**
     * If this session can delete this step.
     */
    boolean canDeleteStep(CoreSession session);

    /**
     * If this step can be undone. Default is to allow undoing only if the parent folder is running.
     */
    boolean canUndoStep(CoreSession session);

    /**
     * make this user or group step deleter.
     */
    void setCanDeleteStep(CoreSession session, String userOrGroup);

    /**
     * Set the step back to the ready state from running or done. This method only modify the step state, it does not
     * run any other action (such as undoing the step action)
     */
    void backToReady(CoreSession session);

    /**
     * Set the step to a cancel step. This method only modify the state of this element and does not run any other
     * action.
     */
    void setCanceled(CoreSession session);

    /**
     * Cancel this element.
     */
    void cancel(CoreSession session);

    boolean isCanceled();

    /**
     * @return true
     */
    boolean isModifiable();

    /**
     * @since 7.2
     */
    String getTitle();
}
