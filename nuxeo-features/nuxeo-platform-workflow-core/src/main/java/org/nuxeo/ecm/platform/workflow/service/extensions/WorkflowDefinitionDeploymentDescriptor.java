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
 * $Id: WorkflowDefinitionDeploymentDescriptor.java 20788 2007-06-19 08:16:55Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.service.extensions;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * Workflow definition deployment descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("definition")
public class WorkflowDefinitionDeploymentDescriptor {

    // this is set by the type service to the context that knows how to locate
    // the definitions
    public RuntimeContext context;

    @XNode("engineName")
    private String engineName;

    @XNode("definitionPath")
    private String definitionPath;

    @XNode("mimetype")
    private String mimetype;


    public String getDefinitionPath() {
        return definitionPath;
    }

    public void setDefinitionPath(String definitionPath) {
        this.definitionPath = definitionPath;
    }

    public String getEngineName() {
        return engineName;
    }

    public void setEngineName(String engineName) {
        this.engineName = engineName;
    }

    public String getMimetype() {
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

}
