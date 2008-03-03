/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: StartupActionHandler.java 22880 2007-07-23 12:13:55Z npaslaru $
 */

package org.nuxeo.ecm.platform.workflow.jbpm.handlers;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowEventTypes;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;

/**
 * Default startup action handler.
 *
 * <p>
 * Invoked at workflow startup time.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class StartupActionHandler extends AbstractWorkflowDocumentActionHandler {

    private static final long serialVersionUID = 2751273934453742494L;


    public void execute(ExecutionContext ec) throws Exception {
        log.info("             PROCESS STARTS               ");

        // Bind document to process
        bindDocumentToProcess(ec);

        // Setup default rights.
        setupDefaultRightsFromPolicy(ec);

  /*   Obsolete - notification changed - to be removed
   *    //send event only for approbation review
        log.debug("Approbation review Started !");

        log.debug("Review : " + ec.getProcessDefinition().getName());
        if ("document_review_approbation".equals(ec.getProcessDefinition().getName())) {
            if ("start".equals(ec.getTransitionSource().getName())) {
                log.debug("Send notification event");
                notifyEvent(ec, WorkflowEventTypes.APPROBATION_WORKFLOW_STARTED);
            }
  */
    }

}
