/*
 * (C) Copyright 2010-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ldoguin
 */
package org.nuxeo.ecm.platform.task;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author Laurent Doguin
 * @since 5.5
 */
public interface TaskProvider extends Serializable {

    /**
     *
     * @param coreSession
     * @return A list of task instances visible by the coreSession's user.
     * @throws IllegalStateException If the currentUser is null.
     */
    List<Task> getCurrentTaskInstances(CoreSession coreSession)
            throws ClientException;

    /**
     * Returns a list of task instances assigned to one of the actors in the
     * list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     * @param coreSession
     * @return
     * @throws ClientException
     */
    List<Task> getCurrentTaskInstances(List<String> actors,
            CoreSession coreSession) throws ClientException;

    /**
     * Returns the list of task instances associated with this document for
     * which the user is the actor or belongs to the pooled actor list.
     * <p>
     * If the user is null, then it returns all task instances for the document.
     *
     * @param dm the document.
     * @param user
     * @param coreSession
     * @return
     * @throws ClientException
     */
    List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            CoreSession coreSssion) throws ClientException;

    /**
     * Returns the list of task instances associated with this document assigned
     * to one of the actor in the list or its pool.
     *
     * @param dm
     * @param actors
     * @param coreSession
     * @return
     * @throws ClientException
     */
    List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            CoreSession coreSession) throws ClientException;

}
