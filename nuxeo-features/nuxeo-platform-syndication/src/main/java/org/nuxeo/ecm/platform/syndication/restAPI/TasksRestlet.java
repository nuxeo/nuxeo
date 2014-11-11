/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bchaffangeon
 *
 */

package org.nuxeo.ecm.platform.syndication.restAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.syndication.serializer.ResultSummary;
import org.nuxeo.ecm.platform.syndication.serializer.SerializerHelper;
import org.nuxeo.ecm.platform.syndication.workflow.DashBoardItem;
import org.nuxeo.ecm.platform.syndication.workflow.DashBoardItemImpl;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * @author bchaffangeon
 * @author arussel
 */
public class TasksRestlet extends BaseStatelessNuxeoRestlet {

    private static final Log log = LogFactory.getLog(TasksRestlet.class);

    private static final String defaultFormat = "XML";

    private JbpmService jbpmService;

    public JbpmService getJbpmService() throws Exception {
        if (jbpmService == null) {
            jbpmService = Framework.getService(JbpmService.class);
        }
        return jbpmService;
    }

    @Override
    public void handle(Request request, Response response) {

        String repo = (String) request.getAttributes().get("repo");

        if (!initRepository(response, repo)) {
            return;
        }

        String format = request.getResourceRef().getQueryAsForm().getFirstValue(
                "format");
        if (format == null) {
            format = defaultFormat;
        }

        String lang = request.getResourceRef().getQueryAsForm().getFirstValue(
                "lang");

        String myTasksRequested = request.getResourceRef().getQueryAsForm().getFirstValue(
                "mytasks");
        boolean wantMyTasks = true;
        if ("false".equalsIgnoreCase(myTasksRequested)) {
            wantMyTasks = false;
        }

        // labels to translate
        List<String> labels = new LinkedList<String>();
        if (lang != null) {
            String allLabels = request.getResourceRef().getQueryAsForm().getFirstValue(
                    "labels");
            if (allLabels != null) {
                for (String label : allLabels.split("\\,")) {
                    // if (!label.startsWith("label.")) {
                    // label = "label." + label;
                    // }
                    labels.add(label);
                }
            }
        }

        List<DashBoardItem> dashboardItems = null;
        try {
            dashboardItems = getDashboardItemsForUser(
                    (NuxeoPrincipal) getUserPrincipal(request), repo, response,
                    wantMyTasks);
        } catch (Exception e1) {
            handleError(response, e1);
        }

        ResultSummary summary = new ResultSummary();
        summary.setTitle("Tasks for " + getUserPrincipal(request).getName());
        summary.setLink(getRestletFullUrl(request));

        try {
            SerializerHelper.formatResult(summary, dashboardItems, response,
                    format, null, getHttpRequest(request), labels, lang);
        } catch (ClientException e) {
            handleError(response, e);
        }
    }

    private List<DashBoardItem> getDashboardTaskItemsForUser(
            NuxeoPrincipal user, String repository, Response response)
            throws Exception {

        List<DashBoardItem> results = new ArrayList<DashBoardItem>();
        List<TaskInstance> tasks = getJbpmService().getCurrentTaskInstances(
                user, getFilter());
        if (tasks != null) {
            for (TaskInstance task : tasks) {
                DocumentModel doc = null;
                try {
                    doc = getJbpmService().getDocumentModel(task, user);
                } catch (Exception e) {
                    log.warn(e, e);
                }
                if (doc != null) {
                    results.add(new DashBoardItemImpl(task, doc));
                } else {
                    log.warn(String.format(
                            "User '%s' has a task of type '%s' on an "
                                    + "unexisting or invisible document",
                            user.getName(), task.getName()));
                }
            }
        }
        return results;
    }

    private List<DashBoardItem> getDashboardItemsManagedByUser(
            NuxeoPrincipal user, String repository, Response response)
            throws Exception {

        List<DashBoardItem> results = new ArrayList<DashBoardItem>();
        List<ProcessInstance> processes = getJbpmService().getCurrentProcessInstances(
                user, getFilter());
        if (processes != null) {
            for (ProcessInstance process : processes) {
                DocumentModel doc = null;
                try {
                    doc = getJbpmService().getDocumentModel(process, user);
                } catch (Exception e) {
                    log.warn(e, e);
                }
                if (doc != null) {
                    Token token = process.getRootToken();
                    Collection<TaskInstance> notDone = jbpmService.getTaskInstances(
                            process.getId(), null, null);
                    for (TaskInstance task : notDone) {
                        Set<PooledActor> actors = task.getPooledActors();
                        StringBuilder names = new StringBuilder();
                        if (actors.isEmpty()) {
                            names.append("Workflow Not Started");
                        } else {
                            for (PooledActor actor : actors) {
                                if (!names.toString().equals("")) {
                                    names.append(",");
                                }
                                String id = actor.getActorId();
                                if (id.indexOf(':') == -1) {
                                    log.error("Unable to find to find a ':' in actor id:"
                                            + id);
                                    names.append(id);
                                } else {
                                    names.append(id.substring(id.indexOf(':') + 1));
                                }
                            }
                        }
                        DashBoardItem item = new DashBoardItemImpl(task, doc);
                        item.prependToComment(names.toString());
                        results.add(item);
                    }
                } else {
                    log.warn(String.format(
                            "User '%s' has a process id of '%ld' on an "
                                    + "unexisting or invisible document",
                            user.getName(), process.getId()));
                }
            }
        }
        return results;

    }

    private List<DashBoardItem> getDashboardItemsForUser(NuxeoPrincipal user,
            String repository, Response response, boolean myTasks)
            throws Exception {
        List<DashBoardItem> currentUserTasks = new ArrayList<DashBoardItem>();
        if (myTasks) {
            currentUserTasks = getDashboardTaskItemsForUser(user, repository,
                    response);
        } else {
            // people I am waiting on
            currentUserTasks = getDashboardItemsManagedByUser(user, repository,
                    response);
        }

        return currentUserTasks;
    }

    public static JbpmListFilter getFilter() {
        return null;
    }

    @Override
    protected boolean initRepository(Response res, String repoId) {

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        RepositoryManager rm;
        try {
            rm = Framework.getService(RepositoryManager.class);
        } catch (Exception e1) {
            handleError(result, res, e1);
            return false;
        }

        Repository repo = null;
        if (repoId == null) {
            repo = rm.getDefaultRepository();
        } else {
            repo = rm.getRepository(repoId);
        }

        if (repo == null) {
            handleError(res, "Unable to get " + repoId + " repository");
            return false;
        }

        try {
            session = repo.open();
        } catch (Exception e1) {
            handleError(result, res, e1);
            return false;
        }
        if (session == null) {
            handleError(result, res, "Unable to open " + repoId + " repository");
            return false;
        }
        return true;
    }

}
