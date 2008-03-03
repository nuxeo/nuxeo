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
 * $Id: AbstractWorkflowDocumentAssignmentHandler.java 28515 2008-01-06 20:37:29Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client;

import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.common.AbstractWorkflowDocumentHandler;

/**
 * Abstract workflow document assignment handler.
 * <p>
 * This class aims at being used when implementing business assignment handlers
 * bound to jBPM business processes deployed along with Nuxeo core. It provides
 * a higher-level API to perform default document oriented operations under
 * the supervision of the process.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractWorkflowDocumentAssignmentHandler extends
        AbstractWorkflowDocumentHandler implements AssignmentHandler {

    protected AbstractWorkflowDocumentAssignmentHandler() {
    }

}
