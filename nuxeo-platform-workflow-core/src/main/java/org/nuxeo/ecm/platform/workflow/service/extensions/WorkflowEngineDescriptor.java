/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.workflow.service.extensions;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;

/**
 * Workflow engine descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("engine")
public class WorkflowEngineDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<WorkflowEngine> className;

    @XNode("@default")
    private boolean defaultEngine;

    public WorkflowEngineDescriptor() {
    }

    public WorkflowEngineDescriptor(String name, Class<WorkflowEngine> className) {
        this.name = name;
        this.className = className;
    }

    public boolean isDefaultEngine() {
        return defaultEngine;
    }

    public void setDefaultEngine(boolean defaultEngine) {
        this.defaultEngine = defaultEngine;
    }

    public Class<WorkflowEngine> getClassName() {
        return className;
    }

    public void setClassName(Class<WorkflowEngine> className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
