/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.runtime.jboss.deployer.structure;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.vfs.plugins.structure.jar.JARStructure;
import org.jboss.deployers.vfs.spi.structure.StructureContext;

/**
 * Avoid letting JARStructure deploy JARs containing "nuxeo.war" because they
 * will try to deploy the war...
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebBundleStructureDeployer extends JARStructure {

    public WebBundleStructureDeployer() {
        setRelativeOrder(10);
    }

    @Override
    public boolean determineStructure(StructureContext structureContext)
            throws DeploymentException {
        if (super.determineStructure(structureContext)) {
            if (structureContext.getMetaData().getContext("nuxeo.war") != null) {
                System.out.println(structureContext.getFile());
                structureContext.getMetaData().removeContext("nuxeo.war");
            }
            return true;
        }
        return false;
    }
}
