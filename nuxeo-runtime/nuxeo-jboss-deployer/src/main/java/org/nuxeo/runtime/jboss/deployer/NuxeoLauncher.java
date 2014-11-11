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
import java.util.Map;
import java.util.Properties;

import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.virtual.VirtualFile;
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

    public static final String NUXEO_HOME_DIR = "nuxeo.home.dir";

    public static final String NUXEO_EAR_DIR = "nuxeo.ear.dir";

    protected File home;

    public NuxeoLauncher() {
        setStage(DeploymentStages.INSTALLED);
    }

    public File getNuxeoHome() {
        return home;
    }

    /**
     * This method is needed for optimization in the zipped ear deployment. It
     * tries to get the original zip file to be able to check the last modified
     * time to see if the unzip is required.
     *
     * @param unit
     * @return
     * @throws Exception
     */
    public File getRealNuxeoZipFile(VFSDeploymentUnit unit) throws Exception {
        String name = unit.getName();
        int p = name.indexOf(':');
        if (p > -1) {
            name = name.substring(p + 1);
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        File file = new File(name);
        return file.isFile() ? file : null;
    }

    @Override
    protected void doDeploy(VFSDeploymentUnit unit) throws Exception {
        DeploymentStructure md = NuxeoStructureDeployer.popStructure(unit.getRoot().getName());
        if (md == null) {
            VirtualFile root = unit.getRoot();
            md = NuxeoStructureDeployer.accept(root);
            if (md == null) {
                return;
            }
            // try to optimize unzipping by using the last modified time of the
            // original zip it the location is known.
            File realzip = getRealNuxeoZipFile(unit);
            md.initialize(realzip != null ? realzip.lastModified() : 0);
        }
        ClassLoader cl = unit.getClassLoader();
        home = md.getHome();
        log.info("Launching Nuxeo from: " + home);
        Map<String, String> props = md.getProperties();
        Properties sysprops = System.getProperties();
        sysprops.setProperty(NUXEO_EAR_DIR, home.getAbsolutePath());
        String v = props.get(NUXEO_HOME_DIR);
        if (v != null) {
            v = Utils.expandVars(v, sysprops);
            home = new File(v);
            home.mkdirs();
        }
        File[] bundles = md.getResolvedBundleFiles();
        Bootstrap b = new Bootstrap(home, Arrays.asList(bundles), cl);
        b.getProperties().putAll(props);
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
