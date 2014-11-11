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
package org.nuxeo.runtime.jboss.deployer.debug;

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.deployment.ListDeploymentUnitFilter;
import org.jboss.virtual.VirtualFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ScanFilter extends ListDeploymentUnitFilter {

    protected String appName = "nuxeo.ear";

    public ScanFilter() {
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }

    @Override
    public boolean accepts(VFSDeploymentUnit unit) {
        // String path = unit.getRoot().getPathName();
        try {
            VirtualFile pfile = unit.getRoot().getParent();
            if (pfile != null) {
                if (appName.equals(pfile.getName())
                        && "datasources".equals(unit.getRoot().getName())) {
                    // ignore all nuxeo.ear submodules
                    // System.out.println("###### IGNORE unit for scanning: " +
                    // path);
                    return false;
                    // } else if (pfile.getName().endsWith(".jar")
                    // && "nuxeo.war".equals(unit.getName())) {
                    // return false;
                }
            }
            // System.out.println("my filter: " + unit.getRoot().getName()
            // + " ==> " + path);
            return super.accepts(unit);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
