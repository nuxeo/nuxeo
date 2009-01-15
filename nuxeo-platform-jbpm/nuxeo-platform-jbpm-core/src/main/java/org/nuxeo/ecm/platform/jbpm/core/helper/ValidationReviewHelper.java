/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.helper;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.platform.jbpm.JbpmService;

/**
 * @author arussel
 *
 */
public class ValidationReviewHelper extends AbstractHelper {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeHasStarted()) {
            String user = getSwimlaneUser(JbpmService.VariableName.initiator.name());
            String endLifecycle = getStringVariable(JbpmService.VariableName.endLifecycle.name());
            if (endLifecycle != null && !"".equals(endLifecycle)) {
                followTransition(user, endLifecycle);
            }
        }
        executionContext.getToken().signal();
    }
}
