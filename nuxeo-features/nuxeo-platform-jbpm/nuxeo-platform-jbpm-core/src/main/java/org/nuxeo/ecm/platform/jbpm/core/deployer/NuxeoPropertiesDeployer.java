/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.jbpm.core.deployer;

import java.io.Serializable;
import java.net.URL;

import org.nuxeo.ecm.platform.jbpm.AbstractProcessDefinitionDeployer;
import org.nuxeo.runtime.api.Framework;

/**
 * @author arussel
 *
 */
public class NuxeoPropertiesDeployer extends AbstractProcessDefinitionDeployer
        implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String DEPLOY_PROCESS_DEFINITION = "org.nuxeo.ecm.platform.jbpm.deployProcessDefinition";

    @Override
    public boolean isDeployable(URL url) throws Exception {
        return Boolean.parseBoolean(Framework.getProperty(
                DEPLOY_PROCESS_DEFINITION, "true"));
    }
}
