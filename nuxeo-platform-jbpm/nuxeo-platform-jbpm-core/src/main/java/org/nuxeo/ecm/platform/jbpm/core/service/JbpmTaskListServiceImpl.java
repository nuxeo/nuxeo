/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nicolas Ulrich
 *
 */

package org.nuxeo.ecm.platform.jbpm.core.service;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskListService;
import org.nuxeo.ecm.platform.jbpm.TaskList;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class JbpmTaskListServiceImpl extends DefaultComponent implements
        JbpmTaskListService {

    private static final String JBPMLISTS = "tasklists";

    public TaskList createTaskList(CoreSession session, String listName)
            throws ClientException {

        // Retrieve root list
        DocumentModel listRoot = getOrCreateListRoot(session);

        // Create a list
        DocumentModel taskListDoc = session.createDocumentModel(
                listRoot.getPathAsString(), listName, "TaskList");
        taskListDoc = session.createDocument(taskListDoc);

        // Save it
        taskListDoc = session.saveDocument(taskListDoc);
        session.save();

        // Return the TaskList Adapter
        return taskListDoc.getAdapter(TaskList.class);

    }

    public void saveTaskList(CoreSession session, TaskList list)
            throws ClientException {

        DocumentModel listDoc = list.getDocument();
        session.saveDocument(listDoc);
        session.save();

    }

    public TaskList getTaskList(CoreSession session, String listUUId)
            throws ClientException {

        TaskList list = null;

        if (session.exists(new IdRef(listUUId))) {

            DocumentModel listDoc = session.getDocument(new IdRef(listUUId));
            list = listDoc.getAdapter(TaskList.class);

        }

        return list;

    }

    public void deleteTaskList(CoreSession session, String listUUId)
            throws ClientException {

        if (session.exists(new IdRef(listUUId))) {
            session.removeDocument(new IdRef(listUUId));
            session.save();
        }

    }

    /**
     * Return the personal Workspace.
     *
     * @param session Current CoreSession
     * @return The personal Workspace
     * @throws ClientException
     */
    private static DocumentModel getUserWorkspace(CoreSession session)
            throws ClientException {

        UserWorkspaceService uws = Framework.getLocalService(UserWorkspaceService.class);
        DocumentModel userWorkspace = uws.getCurrentUserPersonalWorkspace(
                session, null);
        return userWorkspace;

    }

    /**
     * Return the folder which contains the lists of tasks. This folder is
     * named 'jbpmlists' and is located in the personal workspace. *
     *
     * @param session Current CoreSession
     * @return The folder containing the lists of tasks
     * @throws ClientException
     */
    private static DocumentModel getOrCreateListRoot(CoreSession session)
            throws ClientException {

        String path = String.format("%s/%s",
                getUserWorkspace(session).getPathAsString(), JBPMLISTS);

        DocumentModel taskListRootDoc = null;

        if (session.exists(new PathRef(path))) {

            taskListRootDoc = session.getDocument(new PathRef(path));

        } else {

            // Create Root List
            taskListRootDoc = session.createDocumentModel(getUserWorkspace(
                    session).getPathAsString(), JBPMLISTS, "TaskLists");

            taskListRootDoc = session.createDocument(taskListRootDoc);

            session.saveDocument(taskListRootDoc);
            session.save();

        }

        return taskListRootDoc;

    }

    public List<TaskList> getTaskLists(CoreSession session)
            throws ClientException {

        List<TaskList> taskLists = new ArrayList<TaskList>();

        DocumentModel listRoot = getOrCreateListRoot(session);

        DocumentModelList docs = session.getChildren(listRoot.getRef(),
                "TaskList");

        for (DocumentModel documentModel : docs) {
            taskLists.add(documentModel.getAdapter(TaskList.class));
        }

        return taskLists;
    }
}
