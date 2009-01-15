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

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy;

/**
 * @author arussel
 *
 */
public class DefaultJbpmSecurityPolicy implements JbpmSecurityPolicy {

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.jbpm.core.PermissionMapper#getPermissionName(org
     *      .jbpm.graph.def.Transition,
     *      org.nuxeo.ecm.platform.jbpm.core.PermissionMapper.Action)
     */
    public Boolean checkPermission(Transition transition, Action action,
            DocumentModel dm) {
        return Boolean.TRUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.platform.jbpm.core.PermissionMapper#getPermissionName(org
     *      .jbpm.graph.def.ProcessDefinition,
     *      org.nuxeo.ecm.platform.jbpm.core.PermissionMapper.Action)
     */
    public Boolean checkPermission(ProcessDefinition processDefinition,
            Action action, DocumentModel dm) {
        return Boolean.TRUE;
    }

}
