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
 *     arussel
 */
package org.nuxeo.ecm.platform.routing.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * An element of a {@link DocumentRoute}
 *
 * @author arussel
 *
 */
public interface DocumentRouteElement extends Serializable {

    /**
     * The lifecycle state of an element
     *
     */
    enum ElementLifeCycleState {
        draft, validated, ready, running, done
    }

    /**
     * The transition of the lifecycle state.
     *
     */
    enum ElementLifeCycleTransistion {
        toValidated, toReady, toRunning, toDone
    }

    /**
     * Return the list of documents that this route processes.
     *
     * @param session the session used to fetch the documents
     * @return
     */
    DocumentModelList getAttachedDocuments(CoreSession session);

    /**
     * Return the DocumentRoute this element is part of.
     *
     * @param session The session use to fetch the route.
     * @return
     */
    DocumentRoute getDocumentRoute(CoreSession session);

    /**
     * if the route this element is part of has been validated.
     *
     * @return
     */
    boolean isValidated();

    /**
     * if this element is ready.
     *
     * @return
     */
    boolean isReady();

    /**
     * if this route is done.
     *
     * @return
     */
    boolean isDone();

    /**
     * if this route is running.
     *
     * @return
     */
    boolean isRunning();

    /**
     * The name of this element.
     *
     * @return
     */
    String getName();

    /**
     * the description of this element.
     *
     * @return
     */
    String getDescription();

    /**
     * Execute this element. If this is a step, it will run the operation, if
     * this is a containter it will run its children.
     *
     * @param session
     * @return true is the element is not done
     */
    void run(CoreSession session);

    /**
     * Validate this element.
     *
     * @param session
     * @throws ClientException
     */
    void validate(CoreSession session) throws ClientException;

    DocumentModel getDocument();

    /**
     * save the document representing this DocumentRoute.
     *
     * @param session
     */
    void save(CoreSession session);

    void setValidated(CoreSession session);

    void setReady(CoreSession session);

    void setRunning(CoreSession session);

    void setDone(CoreSession session);

    void setReadOnly(CoreSession session) throws ClientException;

    void followTransition(ElementLifeCycleTransistion transition,
            CoreSession session, boolean recursive);

    boolean canValidateStep(CoreSession session);

    void setCanValidateStep(CoreSession session, String userOrGroup);

    boolean canUpdateStep(CoreSession session);

    void setCanUpdateStep(CoreSession session, String userOrGroup);

    boolean canDeleteStep(CoreSession session);

    void setCanDeleteStep(CoreSession session, String userOrGroup);
}
