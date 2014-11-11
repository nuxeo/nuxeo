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
 * $Id: WorkItemsListsBusinessDelegate.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListsManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WorkItemsListsBusinessDelegate implements Serializable {

    private static final long serialVersionUID = -4741037421909876717L;

    private static final Log log = LogFactory.getLog(WorkflowRulesBusinessDelegate.class);

    protected WorkItemsListsManager wiLists;

    public WorkItemsListsManager getWorkItemLists() throws Exception {
        log.debug("getWorkflowRules()");
        if (wiLists == null) {
            wiLists = Framework.getService(WorkItemsListsManager.class);
        }
        log.debug("WAPI bean found :" + wiLists.getClass().toString());
        return wiLists;
    }

}
