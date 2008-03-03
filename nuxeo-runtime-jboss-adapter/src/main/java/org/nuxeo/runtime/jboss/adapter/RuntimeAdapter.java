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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.runtime.jboss.adapter;


import java.io.File;
import java.util.Collection;

import org.jboss.system.ListenerServiceMBeanSupport;
import org.nuxeo.runtime.NXRuntime;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.Version;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.osgi.OSGiRuntimeContext;

/**
 * Adapts the runtime service to a MBean.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RuntimeAdapter extends ListenerServiceMBeanSupport implements RuntimeAdapterMBean {

    public static final String NAME = "nx:type=service,name=RuntimeAdapter";

    private File tempDeployDir;


    public RuntimeService getRuntime() {
        return Framework.getRuntime();
    }

    public String getHomeLocation() {
        if (!NXRuntime.isInitialized()) {
            return "";
        }
        File home = Framework.getRuntime().getHome();
        return home == null ? "" : home.toString();
    }

    public String listResolvedComponents() {
        Collection<RegistrationInfo> regs =  Framework.getRuntime()
            .getComponentManager().getRegistrations();
        StringBuilder buf = new StringBuilder();
        for (RegistrationInfo reg : regs) {
            buf.append(reg.getName());
            if (reg.getContext() instanceof OSGiRuntimeContext) {
                buf.append(" [ from bundle ")
                    .append(reg.getContext().getBundle().getSymbolicName())
                    .append(" ]\n");
            } else {
                buf.append('\n');
                // TODO: put xml path?
            }
        }
        return buf.toString();
    }

    public String listPendingComponents() {
        Collection<ComponentName> regs =  Framework.getRuntime()
            .getComponentManager().getPendingRegistrations();
        StringBuilder buf = new StringBuilder();
        for (ComponentName reg : regs) {
            buf.append(reg).append('\n');
            // TODO: get pending cause details
        }
        return buf.toString();
    }


    public String getDescription() {
        if (!NXRuntime.isInitialized()) {
            return "";
        }
        return Framework.getRuntime().getDescription();
    }

    public Version getVersion() {
        if (!NXRuntime.isInitialized()) {
            return new Version(0, 0, 0);
        }
        return Framework.getRuntime().getVersion();
    }

    public String listExtensionPoints() {
        // TODO Auto-generated method stub
        return null;
    }

    public String listServices() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return the tempDeployDir
     */
    public File getTempDeployDir() {
        return tempDeployDir;
    }

    @Override
    protected void createService() throws Exception {
        super.createService();
        tempDeployDir = Framework.getRuntime().getHome();
        tempDeployDir = new File(tempDeployDir, "deploy");
        tempDeployDir.mkdirs();
        tempDeployDir.deleteOnExit();
    }

}
