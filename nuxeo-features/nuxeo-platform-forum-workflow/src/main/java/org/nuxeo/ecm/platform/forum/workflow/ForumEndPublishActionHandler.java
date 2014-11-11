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
 * $Id: ForumEndPublishActionHandler.java 20771 2007-06-18 21:04:16Z sfermigier $
 */

package org.nuxeo.ecm.platform.forum.workflow;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public class ForumEndPublishActionHandler extends
        AbstractForumWorkflowDocumentHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
        log.debug("Moderation process end");
        // FIXME: get rid of these two try/except blocks?
        try {
            // Here we change the lifeCycleState of the post
            documentFollowTransition(executionContext,
                    ForumConstants.TRANSITION_TO_PUBLISHED_STATE);

            ProcessInstance pi = getProcessInstance(executionContext);
            // Unbind document to process
            unbindDocumentToProcess(executionContext);
            // End process instance
            pi.end();
            // Remove WF ACL
            // removeWFACL(executionContext);
            try {
                notifyEvent(executionContext, null);
            } catch (Exception we) {
                we.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
