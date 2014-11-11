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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.jbpm.JbpmSecurityPolicy;

/**
 * @author arussel
 *
 */
@XObject("securityPolicy")
public class SecurityPolicyDescriptor {

    @XNode("@class")
    private Class<? extends JbpmSecurityPolicy> klass;

    @XNode("@for")
    private String processDefinition;

    public Class<? extends JbpmSecurityPolicy> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends JbpmSecurityPolicy> klass) {
        this.klass = klass;
    }

    public String getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(String processDefinition) {
        this.processDefinition = processDefinition;
    }

}
