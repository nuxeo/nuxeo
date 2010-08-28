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

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 */
@Name("org.nuxeo.ecm.platform.jbpm.web.JbpmServiceBean")
@Scope(ScopeType.APPLICATION)
public class JbpmServiceBusinessDelegate {

    @Factory(value = "jbpmService", scope = ScopeType.APPLICATION)
    public JbpmService bpmManagementServiceFactory() throws Exception {
        return Framework.getService(JbpmService.class);
    }

}
