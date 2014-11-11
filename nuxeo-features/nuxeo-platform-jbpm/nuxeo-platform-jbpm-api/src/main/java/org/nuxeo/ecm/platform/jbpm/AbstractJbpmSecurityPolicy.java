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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm;

import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmService.VariableName;

/**
 * @author Anahide Tchertchian
 *
 */
public abstract class AbstractJbpmSecurityPolicy implements JbpmSecurityPolicy {

    protected boolean isAdminOrInitiator(ProcessInstance pi, NuxeoPrincipal user) {
        return user != null
                && (user.isAdministrator() || (NuxeoPrincipal.PREFIX + user.getName()).equals(getInitiator(pi)));
    }

    protected String getInitiator(ProcessInstance pi) {
        return getStringVariable(VariableName.initiator.name(), pi);
    }

    protected String getStringVariable(String name, ProcessInstance pi) {
        return (String) pi.getContextInstance().getVariable(name);
    }

}
