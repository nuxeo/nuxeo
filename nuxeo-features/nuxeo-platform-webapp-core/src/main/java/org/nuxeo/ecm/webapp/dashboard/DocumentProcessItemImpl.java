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

import java.util.Date;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;

/**
 * Document process item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentProcessItemImpl implements DocumentProcessItem {

    private static final long serialVersionUID = 1L;

    protected final DocumentModel documentModel;

    protected final WMProcessInstance processInstance;

    protected final String docTitle;

    protected final Date procesinstanceStartDate;

    protected final String processInstanceName;

    public DocumentProcessItemImpl(DocumentModel documentModel,
            WMProcessInstance processInstance) {
        this.documentModel = documentModel;
        this.processInstance = processInstance;
        docTitle = documentModel.getTitle();
        processInstanceName = processInstance.getName();
        procesinstanceStartDate = processInstance.getStartDate();
    }

    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    public WMProcessInstance getProcessInstance() {
       return processInstance;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public Date getProcessInstanceStartDate() {
        return procesinstanceStartDate;
    }

}
