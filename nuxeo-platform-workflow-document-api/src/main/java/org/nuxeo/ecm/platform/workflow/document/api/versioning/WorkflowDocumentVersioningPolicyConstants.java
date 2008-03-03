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
 * $Id: WorkflowDocumentVersioningPolicyConstants.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.versioning;

/**
 * Workflow versioning policy constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkflowDocumentVersioningPolicyConstants {

    /**
     * The user chooses the versioning type when modifying the document.
     * <p>
     * The versioning types allowed are:
     * <ul>
     *   <li>Minor version increment</li>
     *   <li>No increment</li>
     * </ul>
     */
    public static final String WORKFLOW_DOCUMENT_VERSIONING_CASE_DEPENDENT = "workflowDocumentVersioningCaseDependent";

    /**
     * The user can't increment the version at all.
     *
     */
    public static final String WORKFLOW_DOCUMENT_VERSIONING_NO_INCREMENT = "workflowDocumentVersioningNoIncrement";

    /**
     * When the user modifies the document, the version is automatically incremented on the minor number.
     *
     */
    public static final String WORKFLOW_DOCUMENT_VERSIONING_AUTO = "workflowDocumentVersioningAuto";

    private WorkflowDocumentVersioningPolicyConstants() {
    }

}
