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
 * $Id: WorkItemsListException.java 23602 2007-08-08 18:02:51Z janguenot $
 */

package org.nuxeo.ecm.platform.workflow.document.api.workitem;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.api.common.WrappedException;

/**
 * Exceptions-related to work items lists.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkItemsListException extends WMWorkflowException {

    private static final long serialVersionUID = -7998239443364067564L;

    public WorkItemsListException() {
    }

    public WorkItemsListException(String message) {
        super(message);
    }

    public WorkItemsListException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }

    public WorkItemsListException(Throwable cause) {
        super(WrappedException.wrap(cause));
    }

}
