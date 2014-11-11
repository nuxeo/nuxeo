/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.jbpm.web;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Anahide Tchertchian
 */
@Name("org.nuxeo.ecm.platform.jbpm.web.JbpmTaskServiceBean")
@Scope(ScopeType.APPLICATION)
public class JbpmTaskServiceBusinessDelegate {

    @Factory(value = "jbpmTaskService", scope = ScopeType.APPLICATION)
    public JbpmTaskService getJbpmTaskService() throws Exception {
        return Framework.getService(JbpmTaskService.class);
    }

}
