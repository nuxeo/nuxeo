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
 * $Id$
 */

package org.nuxeo.ecm.webapp.dashboard;

import java.io.Serializable;
import java.util.Date;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;

/**
 * Item holding information about a Document under a process.
 *
 * <p>
 * Aimed at being used in Dashboad fragments.
 * </p>
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface DocumentProcessItem extends Serializable {

    /**
     * Returns the document ref for the document bound to the process.
     *
     * @return a document ref instance.
     */
    DocumentModel getDocumentModel();

    /**
     * Returns the process identifier bound to a given documenñt.
     *
     * @return a process instance identifier.
     */
    WMProcessInstance getProcessInstance();

    /**
     * Return the bound document title.
     *
     * @return the document title.
     */
    String getDocTitle();

    /**
     * Returns the process start date.
     *
     * @return the process start date.
     */
    Date getProcessInstanceStartDate();

    /**
     * Returns the process instance name.
     *
     * @return the process instance name.
     */
    String getProcessInstanceName();



}
