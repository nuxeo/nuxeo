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
 * $Id: ForumStartupActionHandler.java 20771 2007-06-18 21:04:16Z sfermigier $
 */
package org.nuxeo.ecm.platform.forum.workflow;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants;

/**
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ForumStartupActionHandler extends
        AbstractForumWorkflowDocumentHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext ec) throws Exception {

        log.info("Moderation started on : "
                + ec.getVariable(WorkflowConstants.DOCUMENT_REF));

        bindDocumentToProcess(ec);

    }

}
