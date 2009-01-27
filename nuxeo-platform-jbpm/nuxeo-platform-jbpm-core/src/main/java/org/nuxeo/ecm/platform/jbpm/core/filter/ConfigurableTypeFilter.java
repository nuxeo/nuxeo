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
package org.nuxeo.ecm.platform.jbpm.core.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jbpm.JbpmContext;
import org.jbpm.graph.def.ProcessDefinition;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */

public class ConfigurableTypeFilter implements JbpmListFilter {

    private static final long serialVersionUID = 1L;

    public ConfigurableTypeFilter() {

    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> filter(JbpmContext jbpmContext,
            DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
        JbpmService jbpmService = null;
        try {
            jbpmService = Framework.getService(JbpmService.class);
        } catch (Exception e) {
            throw new IllegalStateException("Jbpm Service is not deployed.", e);
        }
        Map<String, List<String>> filters = jbpmService.getTypeFilterConfiguration();
        ArrayList<ProcessDefinition> pds = new ArrayList<ProcessDefinition>();
        for (T t : list) {
            ProcessDefinition definition = (ProcessDefinition) t;
            List<String> allowedTypes = filters.get(definition.getName());
            if(allowedTypes == null) {
                continue;
            }
            if(allowedTypes.contains(document.getType())) {
                pds.add(definition);
            }
        }

        return (ArrayList<T>) pds;
    }
}
