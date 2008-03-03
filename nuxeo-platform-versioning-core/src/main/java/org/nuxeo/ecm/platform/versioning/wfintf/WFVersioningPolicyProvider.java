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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;
import org.nuxeo.ecm.platform.versioning.api.WFDocVersioning;
import org.nuxeo.ecm.platform.workflow.document.api.ejb.delegate.WorkflowDocumentVersioningPolicyBusinessDelegate;
import static org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_AUTO;
import static org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_CASE_DEPENDENT;
import static org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyConstants.WORKFLOW_DOCUMENT_VERSIONING_NO_INCREMENT;
import org.nuxeo.ecm.platform.workflow.document.api.versioning.WorkflowDocumentVersioningPolicyManager;

/**
 * This is a business delegate for Versioning Policy provided by the Workflow.
 * Also maps the versioning options defined by the workflow to local
 * <code>VersioningActions</code> constants to keep sepparation between
 * workflow module and versioning module.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public final class WFVersioningPolicyProvider {

    private static final Log log = LogFactory.getLog(WFVersioningPolicyProvider.class);

    private static final Map<String, VersioningActions> wf2Ver = new HashMap<String, VersioningActions>();

    static {
        wf2Ver.put(WORKFLOW_DOCUMENT_VERSIONING_AUTO,
                VersioningActions.ACTION_INCREMENT_MINOR);
        wf2Ver.put(WORKFLOW_DOCUMENT_VERSIONING_CASE_DEPENDENT,
                VersioningActions.ACTION_CASE_DEPENDENT);
        wf2Ver.put(WORKFLOW_DOCUMENT_VERSIONING_NO_INCREMENT,
                VersioningActions.ACTION_NO_INCREMENT);
    }

    // Utility class.
    private WFVersioningPolicyProvider() {
    }

    public static VersioningActions getVersioningPolicyFor(DocumentModel documentModel)
            throws ClientException {
        String wfpol;
        try {
            wfpol = documentModel.getSystemProp(
                    WFDocVersioning.SYSTEM_PROPERTY_NAME_WF_OPTION,
                    String.class);
        } catch (DocumentException e) {
            log.warn("Workflow versioning inc policy not set as document system prop. Error msg: "
                    + e.getMessage());
            return VersioningActions.ACTION_UNDEFINED;
        }

        if (null == wfpol) {
            log.warn("Workflow versioning inc policy system prop is null.");
            return VersioningActions.ACTION_UNDEFINED;
        }

        return wf2Ver.get(wfpol);
    }

    /**
     * Get VersioningPolicy defined by the current workflow for the given
     * document ref.
     * <p>
     * If no versioning policy or no worklfow are associated to the given
     * document ref then <code>null</code> is returned.
     *
     * @param docRef
     * @return the VersioningAction from the workflow or <code>null</code> if
     *         it cannot be obtained because WF service is not available or
     *         return any workflow related error.
     */
    public static String getVersioningPolicyFor(DocumentRef docRef) {

        final String logPrefix = "<getVersioningPolicyFor> ";

        try {
            WorkflowDocumentVersioningPolicyManager wfVersionPolicy = new WorkflowDocumentVersioningPolicyBusinessDelegate().getWorkflowVersioningPolicy();

            final String wfpol = wfVersionPolicy.getVersioningPolicyFor(docRef);

            log.debug(logPrefix + "received option from WF: " + wfpol);

            return wfpol;
        } catch (Exception e) {
            // e.printStackTrace();
            log.error("Cannot get versioning policy from workflow. "
                    + "Error connecting to Workflow service.", e);
        }

        return null;
    }

    public static VersioningActions translateFromWFPolicy(String versioningIncPolicy) {
        return wf2Ver.get(versioningIncPolicy);
    }

}
