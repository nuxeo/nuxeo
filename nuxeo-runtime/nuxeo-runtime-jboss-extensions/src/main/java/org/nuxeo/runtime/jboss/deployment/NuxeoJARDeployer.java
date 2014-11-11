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
 *     bstefanescu
 */
package org.nuxeo.runtime.jboss.deployment;

import java.util.jar.JarFile;

import javax.management.ObjectName;

import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.JARDeployer;
import org.jboss.deployment.SubDeployer;
import org.jboss.deployment.SubDeployerSupport;
import org.jboss.mx.util.ObjectNameFactory;

/**
 * The default {@link JARDeployer} is scanning JARs for embedded deployable files.
 * This is slowing down the deployment.
 * THis implementation is avoiding this scan when not needed
 *  
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoJARDeployer extends SubDeployerSupport implements NuxeoJARDeployerMBean {

    public static final ObjectName NAME = ObjectNameFactory.create("nx:type=deployer,name=NuxeoJARDeployer");
    
    public NuxeoJARDeployer() {
        setEnhancedSuffixes(new String[] {"100:.jar"});
    }
    
    /* (non-Javadoc)
     * @see org.jboss.deployment.SubDeployerSupport#startService()
     */
    @Override
    protected void startService() throws Exception {
        // TODO Auto-generated method stub
        super.startService();
    }
    
    @Override
    public boolean accepts(DeploymentInfo di) {
        if (!super.accepts(di)) {
            return false;
        }
        if (di.shortName.startsWith("nuxeo-")) {
            return true;
        }
        return false;
    }
    
    public SubDeployer getDeployer() {
        return this;
    }
    
    @Override
    protected void addDeployableJar(DeploymentInfo di, JarFile jarFile)
            throws DeploymentException {
        //super.addDeployableJar(di, jarFile);
        System.out.println(">>>>>>>>>> Deploy Bundle : " +di.shortName);
        // do nothing
    }
    
    @Override
    protected void processNestedDeployments(DeploymentInfo di)
            throws DeploymentException {
        System.out.println(">>>>>>>>>> Deploying Bundle : " +di.shortName);
        //super.processNestedDeployments(di);
    }
}
