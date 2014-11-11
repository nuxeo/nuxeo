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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ToEndActionHandler.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.jbpm;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ToEndActionHandler extends JbpmWorkflowActionHandler {

    private static final long serialVersionUID = 1L;

    public void execute(ExecutionContext executionContext) throws Exception {
        ProcessInstance processInstance = executionContext.getProcessInstance();
        processInstance.end();
    }

}
