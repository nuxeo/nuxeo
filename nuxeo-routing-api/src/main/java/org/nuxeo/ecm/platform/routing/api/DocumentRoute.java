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

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * A DocumentRoute model or instance. A route is a set of step that processes
 * documents.
 *
 * If a method is called that change the state of this object, the
 * {@link #save(CoreSession)} method should be called to persist its state.
 *
 * @author arussel
 *
 */
public interface DocumentRoute extends DocumentRouteStepsContainer {

    /**
     * Set the list of id of documents attached to this instance of RouteModel
     * replacing the previous list if any.
     *
     * @param documents
     */
    void setAttachedDocuments(List<String> documentIds);

    /**
     * The list of documents processed by this route.
     *
     * @return
     */
    List<String> getAttachedDocuments();

}
