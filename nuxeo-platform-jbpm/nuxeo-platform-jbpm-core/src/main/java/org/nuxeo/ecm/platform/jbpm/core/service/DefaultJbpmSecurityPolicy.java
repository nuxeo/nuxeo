/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.service;

import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmSecurityPolicy;

/**
 * @author arussel
 *
 */
public class DefaultJbpmSecurityPolicy extends AbstractJbpmSecurityPolicy {

    public Boolean checkPermission(ProcessInstance pi, Action action,
            DocumentModel dm, NuxeoPrincipal principal) {
        if (Action.write.equals(action)) {
            // only initiator and administrator
            if (isAdminOrInitiator(pi, principal)) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}
