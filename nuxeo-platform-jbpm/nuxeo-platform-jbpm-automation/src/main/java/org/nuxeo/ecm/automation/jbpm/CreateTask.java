/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.automation.jbpm;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;

/**
 * Creates a task
 *
 * @author Anahide Tchertchian
 */
@Operation(id = CreateTask.ID, category = Constants.CAT_DOCUMENT, label = "Create task", description = "")
public class CreateTask {

    public final static String ID = "Document.CreateTask";

    private static final Log log = LogFactory.getLog(CreateTask.class);

    public enum OperationTaskVariableName {
        acceptOperationChain, rejectOperationChain, createdFromCreateTaskOperation
    }

    @Context
    protected OperationContext ctx;

    @Context
    protected CoreSession coreSession;

    @Context
    protected JbpmTaskService jbpmTaskService;

    @Param(name = "task name", required = true, order = 0)
    protected String taskName;

    @Param(name = "due date", required = false, order = 1)
    protected Date dueDate;

    @Param(name = "directive", required = false, order = 2)
    protected String directive;

    @Param(name = "comment", required = false, order = 3)
    protected String comment;

    @Param(name = "accept operation chain", required = false, order = 4)
    protected String acceptOperationChain;

    @Param(name = "reject operation chain", required = false, order = 5)
    protected String rejectOperationChain;

    @Param(name = "key for actors prefixed ids", required = false, order = 6)
    protected String keyForActors;

    @Param(name = "additional list of actors prefixed ids", required = false, order = 7)
    protected StringList additionalPrefixedActors;

    @Param(name = "create one task per actor", required = false, values = "true", order = 8)
    protected boolean createOneTaskPerActor = true;

    @OperationMethod
    public DocumentModelList run(DocumentModelList docs) throws Exception {
        DocumentModelListImpl result = new DocumentModelListImpl(
                (int) docs.totalSize());
        for (DocumentModel doc : docs) {
            result.add(run(doc));
        }
        return result;
    }

    @OperationMethod
    @SuppressWarnings("unchecked")
    public DocumentModel run(DocumentModel document) throws Exception {
        Principal pal = coreSession.getPrincipal();
        if (!(pal instanceof NuxeoPrincipal)) {
            throw new OperationException(
                    "Principal is not an instance of NuxeoPrincipal");
        }

        List<String> prefixedActorIds = new ArrayList<String>();
        Object actors = ctx.get(keyForActors);
        if (actors != null) {
            try {
                prefixedActorIds.addAll((List) actors);
            } catch (ClassCastException e) {
                throw new OperationException(String.format(
                        "Invalid key to retrieve a list of prefixed actor "
                                + "ids '%s', value is not correct: %s",
                        keyForActors, actors));
            }
        }

        if (additionalPrefixedActors != null) {
            prefixedActorIds.addAll(additionalPrefixedActors);
        }

        if (prefixedActorIds == null || prefixedActorIds.isEmpty()) {
            // no actors: do nothing
            log.debug("No actors could be resolved => do not create any task");
            return document;
        }

        // create the task, passing operation chains in task variables
        Map<String, Serializable> taskVariables = new HashMap<String, Serializable>();
        taskVariables.put(
                OperationTaskVariableName.createdFromCreateTaskOperation.name(),
                "true");
        if (!StringUtils.isEmpty(acceptOperationChain)) {
            taskVariables.put(
                    OperationTaskVariableName.acceptOperationChain.name(),
                    acceptOperationChain);
        }
        if (!StringUtils.isEmpty(rejectOperationChain)) {
            taskVariables.put(
                    OperationTaskVariableName.rejectOperationChain.name(),
                    rejectOperationChain);
        }

        if (jbpmTaskService == null) {
            throw new OperationException("Service jbpmTaskService not found");
        }
        jbpmTaskService.createTask(coreSession, (NuxeoPrincipal) pal, document,
                taskName, prefixedActorIds, createOneTaskPerActor, directive,
                comment, dueDate, taskVariables);

        return document;
    }

}
