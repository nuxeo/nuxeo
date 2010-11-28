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
package org.nuxeo.runtime.jboss.deployer;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.virtual.VirtualFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractNuxeoDeployer extends AbstractDeployer {

    @Override
    public void deploy(DeploymentUnit unit) throws DeploymentException {
        if (unit.isTopLevel()) {
            if (unit instanceof VFSDeploymentUnit) {
                VFSDeploymentUnit du = (VFSDeploymentUnit) unit;
                try {
                    // if (!du.getRoot().isArchive()) {
                    VirtualFile file = du.getMetaDataFile("nuxeo-structure.xml");
                    if (file != null) {
                        doDeploy(du);
                    }
                    // }
                } catch (Exception e) {
                    throw new DeploymentException(e);
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit unit) {
        if (unit.isTopLevel()) {
            if (unit instanceof VFSDeploymentUnit) {
                VFSDeploymentUnit du = (VFSDeploymentUnit) unit;
                try {
                    if (!du.getRoot().isArchive()) {
                        VirtualFile file = du.getMetaDataFile("nuxeo-structure.xml");
                        if (file != null) {
                            doUndeploy(du);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to stop nuxeo", e);
                }
            }
        }
    }

    protected abstract void doDeploy(VFSDeploymentUnit unit) throws Exception;

    protected void doUndeploy(VFSDeploymentUnit unit) throws Exception {
        // do nothing
    }

}
