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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client;

import org.jbpm.graph.def.ActionHandler;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.common.AbstractWorkflowDocumentHandler;

/**
 * Abstract workflow document action handler.
 * <p>
 * This class aimed at being used while implementing business action handlers
 * bound to JBPM business processes deployed along with Nuxeo core. It provides
 * a more high level API to perform default document oriented operations under
 * the supervision of the process.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractWorkflowDocumentActionHandler extends
        AbstractWorkflowDocumentHandler implements ActionHandler {

    private static final long serialVersionUID = -745177382248193359L;

    protected AbstractWorkflowDocumentActionHandler() {
    }

}
