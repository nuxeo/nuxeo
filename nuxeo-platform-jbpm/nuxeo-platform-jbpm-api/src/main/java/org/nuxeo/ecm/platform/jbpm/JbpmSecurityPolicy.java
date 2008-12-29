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

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author arussel
 *
 */
public interface JbpmSecurityPolicy {
    enum Action {
        read, write, execute
    };

    /**
     * check the permission on a transition.
     * <dl>
     *   <dt>read</dt><dd>Read a transition.</dd>
     *   <dt>execute</dt><dd>Take a transition.</dd>
     * </dl>
     * @param transition the transition.
     * @param action the action.
     * @param dm the attached document, <code>null</code> if no document is attached to the process.
     * @return if the permission is granted, <code>null</code> if unknown.
     */
    Boolean checkPermission(Transition transition, Action action, DocumentModel dm);

    /**
     * check the permission on a process instance.
     * <dl>
     *   <dt>read</dt><dd>Read a process definition.</dd>
     *   <dt>execute</dt><dd>Create a process instance from this definition.</dd>
     * </dl>
     * @param processDefinition
     * @param action
     * @param dm the attached document, <code>null</code> if no document is attache to the process.
     * @return if the permission is granted, <code>null</code> if unknow.
     */
    Boolean checkPermission(ProcessDefinition processDefinition, Action action, DocumentModel dm);
}
