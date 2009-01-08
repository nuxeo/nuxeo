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
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow;

import org.nuxeo.ecm.platform.workflow.service.WorkflowService;
import org.nuxeo.ecm.platform.workflow.service.WorkflowServiceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Facade for services provided by NXWorkflow module.
 * <p>
 * This is the main entry point to the workflow services
 *
 * @see WorkflowService
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public final class NXWorkflow {

    private NXWorkflow() {
    }

    /**
     * Returns the workflow service.
     *
     * @see WorkflowService
     *
     * @return the workflow service.
     */
    public static WorkflowService getWorkflowService() {
        return (WorkflowService) Framework.getRuntime().getComponent(
                WorkflowServiceImpl.NAME);
    }

}
