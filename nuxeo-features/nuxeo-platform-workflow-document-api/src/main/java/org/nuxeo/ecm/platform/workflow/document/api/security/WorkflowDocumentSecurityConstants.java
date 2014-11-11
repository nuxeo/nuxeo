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
 * $Id: WorkflowDocumentSecurityConstants.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api.security;

import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Workflow related security constants.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class WorkflowDocumentSecurityConstants {

    // :XXX: The permissions below are bound to the Nuxeo Core permissions
    // because Nuxeo core doesn't support dynamic permissions and / or group of
    // permissions.

    public static final String WORKFLOW_PARTICIPANT = SecurityConstants.VIEW_WORKLFOW;

    public static final String DOCUMENT_MODIFY = SecurityConstants.WRITE;

    public static final String DOCUMENT_VIEW = SecurityConstants.READ;

    private WorkflowDocumentSecurityConstants() {
    }

}
