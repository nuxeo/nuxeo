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
package org.nuxeo.ecm.platform.jbpm.web;

import java.util.ArrayList;

import org.jboss.seam.annotations.Name;
import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmService;

/**
 * @author arussel
 *
 */
@Name("org.nuxeo.ecm.platform.jbpm.web.restrictToInitiatorFilter")
public class RestrictToInitiatorFilter implements JbpmListFilter {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> filter(JbpmContext jbpmContext,
            DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
        ArrayList<ProcessInstance> result = new ArrayList<ProcessInstance>();
        for (T t : list) {
            ProcessInstance pi = (ProcessInstance) t;
            String actorId = pi.getTaskMgmtInstance().getSwimlaneInstance(
                    JbpmService.VariableName.initiator.name()).getActorId();
            if (principal.getName().equals(actorId)) {
                result.add(pi);
            }
        }
        return (ArrayList<T>) result;
    }

}
