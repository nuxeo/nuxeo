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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: ReviewEventObserver.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.web.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.document.api.workitem.WorkItemsListException;

/**
 * Review Event Observer.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ReviewEventObserver extends Serializable {

    void updateCurrentLevelAfterDocumentChanged() throws WMWorkflowException;

    void invalidateContextVariables();

    void updateDocumentRights() throws WMWorkflowException;

    void invalidateWorkItemsListsMap() throws WorkItemsListException;

    void invalidateTasksContextVariables();

    void unlockDocument() throws WMWorkflowException;

    void updateDocumentLifecycle() throws ClientException;

}
