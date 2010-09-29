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

import java.io.File;
import java.util.Arrays;

import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.nuxeo.runtime.jboss.deployer.structure.DeploymentStructure;
import org.nuxeo.runtime.jboss.deployer.structure.NuxeoStructureDeployer;

/**
 * Nuxeo Launcher - work both with exploded and zipped Nuxeo EARs. A zipped ear
 * will be unpacked into JBoss tmp folder in nuxeo/[unit-name]
 * 
 * A nuxeo.ear zip will be unpacked only the first time nuxeo starts or if the
 * EAR zip was changed from the last unzip.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class NuxeoLauncher extends AbstractNuxeoDeployer {

    protected File home;

    public NuxeoLauncher() {
        setStage(DeploymentStages.INSTALLED);
    }

    public File getNuxeoHome() {
        return home;
    }

    @Override
    protected void doDeploy(VFSDeploymentUnit unit) throws Exception {
        DeploymentStructure md = NuxeoStructureDeployer.popStructure(unit.getRoot().getName());
        if (md == null) {
            md = NuxeoStructureDeployer.accept(unit.getRoot());
        }
        if (md == null) {
            return;
        }
        ClassLoader cl = unit.getClassLoader();
        home = md.getHome();
        log.info("Launching Nuxeo from: " + home);
        System.getProperties().putAll(md.getProperties());
        File[] bundles = md.getResolvedBundleFiles();
        Bootstrap b = new Bootstrap(home, Arrays.asList(bundles), cl);
        b.getProperties().putAll(md.getProperties());
        b.startNuxeo();
    }

    @Override
    protected void doUndeploy(VFSDeploymentUnit unit) throws Exception {
        ClassLoader cl = unit.getClassLoader();
        File homeDir = home != null ? home
                : Utils.getRealHomeDir(unit.getRoot());
        new Bootstrap(homeDir, null, cl).stopNuxeo();
    }

}
