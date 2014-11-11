/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.task.core.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskProvider;
import org.nuxeo.ecm.platform.task.TaskQueryConstant;
import org.nuxeo.ecm.platform.task.core.helpers.TaskActorsHelper;

/**
 * @author Laurent Doguin
 * @author Antoine Taillefer
 * @since 5.5
 */
public class DocumentTaskProvider implements TaskProvider {

    private static final long serialVersionUID = 1L;

    @Override
    public List<Task> getCurrentTaskInstances(CoreSession coreSession)
            throws ClientException {

        // Get tasks for current user
        // We need to build the task actors list: prefixed and unprefixed names
        // of the principal and all its groups
        NuxeoPrincipal principal = (NuxeoPrincipal) coreSession.getPrincipal();
        List<String> actors = TaskActorsHelper.getTaskActors(principal);

        return getCurrentTaskInstances(actors, coreSession);
    }

    /**
     * Returns a list of task instances assigned to one of the actors in the
     * list or to its pool.
     *
     * @param actors a list used as actorId to retrieve the tasks.
     * @param filter
     * @return
     * @throws ClientException
     */
    @Override
    public List<Task> getCurrentTaskInstances(List<String> actors,
            CoreSession coreSession) throws ClientException {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<Task>();
        }
        String userNames = TaskQueryConstant.formatStringList(actors);
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_ACTORS_QUERY, userNames);
        DocumentModelList taskDocuments = coreSession.query(query);
        return wrapDocModelInTask(taskDocuments);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, NuxeoPrincipal user,
            CoreSession coreSession) throws ClientException {
        String query;
        if (user == null) {
            query = String.format(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_QUERY,
                    dm.getId());
        } else {
            List<String> actors = TaskActorsHelper.getTaskActors(user);
            String userNames = TaskQueryConstant.formatStringList(actors);
            query = String.format(
                    TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY,
                    dm.getId(), userNames);
        }
        DocumentModelList taskDocuments = coreSession.query(query);
        return wrapDocModelInTask(taskDocuments);
    }

    @Override
    public List<Task> getTaskInstances(DocumentModel dm, List<String> actors,
            CoreSession coreSession) throws ClientException {
        if (actors == null || actors.isEmpty()) {
            return new ArrayList<Task>();
        }
        String userNames = TaskQueryConstant.formatStringList(actors);
        String query = String.format(
                TaskQueryConstant.GET_TASKS_FOR_TARGET_DOCUMENT_AND_ACTORS_QUERY,
                dm.getId(), userNames);
        DocumentModelList taskDocuments = coreSession.query(query);
        return wrapDocModelInTask(taskDocuments);
    }

    public static List<Task> wrapDocModelInTask(DocumentModelList taskDocuments) {
        List<Task> tasks = new ArrayList<Task>();
        for (DocumentModel doc : taskDocuments) {
            tasks.add(doc.getAdapter(Task.class));
        }
        return tasks;
    }

}
