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

package org.nuxeo.ecm.platform.jbpm.dashboard;

import java.util.Date;

import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Document process item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class DocumentProcessItemImpl implements DocumentProcessItem {

    private static final long serialVersionUID = 1L;

    protected final DocumentModel documentModel;

    protected final ProcessInstance processInstance;

    protected String docTitle;

    protected final Date processInstanceStartDate;

    protected final String processInstanceName;

    public DocumentProcessItemImpl(ProcessInstance processInstance,
            DocumentModel documentModel) {
        this.documentModel = documentModel;
        this.processInstance = processInstance;
        try {
            docTitle = documentModel.getTitle();
        } catch (ClientException e) {
            docTitle = null;
        }
        processInstanceName = processInstance.getProcessDefinition().getName();
        processInstanceStartDate = processInstance.getStart();
    }

    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public Date getProcessInstanceStartDate() {
        return processInstanceStartDate;
    }

}
