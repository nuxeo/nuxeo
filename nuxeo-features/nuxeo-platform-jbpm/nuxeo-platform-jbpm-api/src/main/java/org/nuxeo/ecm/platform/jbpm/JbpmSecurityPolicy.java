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
package org.nuxeo.ecm.platform.jbpm;

import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * @author arussel
 */
public interface JbpmSecurityPolicy {

    enum Action {
        read, write, execute
    }

    /**
     * Checks the permission on a process instance.
     * <dl>
     * <dt>read</dt>
     * <dd>Read a process instance from this definition.</dd>
     * <dt>write</dt>
     * <dd>Edit a process instance from this definition.</dd>
     * <dt>execute</dt>
     * <dd>Create a process instance from this definition.</dd>
     * </dl>
     *
     * @param processInstance
     * @param action
     * @param dm the attached document, <code>null</code> if no document is
     *            attache to the process.
     * @return if the permission is granted, <code>null</code> if unknown.
     */
    Boolean checkPermission(ProcessInstance processInstance, Action action,
            DocumentModel dm, NuxeoPrincipal user);

}
