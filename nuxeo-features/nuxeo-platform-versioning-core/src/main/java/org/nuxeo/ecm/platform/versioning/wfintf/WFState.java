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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.wfintf;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.versioning.api.WFDocVersioning;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentRelationBusinessDelegate;
import org.nuxeo.ecm.platform.workflow.document.api.relation.WorkflowDocumentRelationManager;

/**
 * Utility class to interface with workflow engine and provide information
 * regarding current workflow processes for the given document.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class WFState {

    private static final Log log = LogFactory.getLog(WFState.class);

    private static WFState instance;

    private WorkflowDocumentRelationBusinessDelegate wdocBusinessDelegate;

    private void init() {
        wdocBusinessDelegate = new WorkflowDocumentRelationBusinessDelegate();
    }

    public WorkflowDocumentRelationManager getWFDocRelationManager()
            throws ClientException {
        WorkflowDocumentRelationManager wfDocRelManager;
        try {
            wfDocRelManager = wdocBusinessDelegate.getWorkflowDocument();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        return wfDocRelManager;
    }

    public static WFState getInstance() {
        if (instance == null) {
            instance = new WFState();
            instance.init();
        }
        return instance;
    }

    public static boolean hasWFProcessInProgress(DocumentRef docRef)
            throws ClientException {
        WorkflowDocumentRelationManager wfDocRelationManager = getInstance().getWFDocRelationManager();
        String[] wfInstanceIds = wfDocRelationManager.getWorkflowInstanceIdsFor(docRef);

        log.debug("doc wf instances: " + Arrays.asList(wfInstanceIds));

        return wfInstanceIds.length != 0;
    }

    public static boolean hasWFProcessInProgress(DocumentModel documentModel) throws ClientException {
        Boolean wfInProgress;
        try {
            wfInProgress = documentModel.getSystemProp(
                    WFDocVersioning.SYSTEM_PROPERTY_WF_IN_PROGRESS,
                    Boolean.class);
        } catch (DocumentException e) {
            log.debug("Workflow wfInProgress not set as document system prop. Error msg: "
                    + e.getMessage());
            return false;
        }
        return wfInProgress;
    }

}
