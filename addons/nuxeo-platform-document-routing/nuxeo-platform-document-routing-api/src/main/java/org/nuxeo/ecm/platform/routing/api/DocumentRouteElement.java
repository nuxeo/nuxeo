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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 *
 * @author arussel
 *
 */
public interface DocumentRouteElement {
    enum ElementLifeCycleState {
        draft, running, done
    }

    enum ElementLifeCycleTransistion {
        toRunning, toDone
    }

    boolean isDone();

    boolean isRunning();

    String getName();

    String getDescription();

    /**
     *
     * @param session
     * @return true is the element is not done
     */
    void run(CoreSession session);

    DocumentModel getDocument();

    /**
     * save the document representing this DocumentRoute.
     *
     * @param session
     */
    void save(CoreSession session);

    void setRunning(CoreSession session);

    void setDone(CoreSession session);
}
