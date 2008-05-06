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

package org.nuxeo.runtime.jboss.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.management.MalformedObjectNameException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.mx.util.MBeanProxyCreationException;
import org.jboss.system.ServiceMBeanSupport;
import org.nuxeo.runtime.ComponentEvent;
import org.nuxeo.runtime.ComponentListener;
import org.nuxeo.runtime.NXRuntime;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jboss.osgi.JBossOSGiAdapter;
import org.nuxeo.runtime.jboss.util.DeploymentHelper;
import org.nuxeo.runtime.jboss.util.ServiceLocator;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * Adapts NXRuntime components to JBoss MBeans.
 * <p>
 * Should be started before the NXRuntime bundle is deployed.
 *
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComponentAdapter extends ServiceMBeanSupport
        implements ComponentAdapterMBean, RuntimeServiceListener, ComponentListener {

    private static final Log log = LogFactory.getLog(ComponentAdapter.class);

    @Override
    protected void createService() throws Exception {
        super.createService();
        // add me as a listener to the current nx runtime service
        // FIXME: use Framework API instead
        NXRuntime.addListener(this);
        if (NXRuntime.isInitialized()) {
            Framework.getRuntime().getComponentManager().addComponentListener(this);
        }
    }

    @Override
    protected void destroyService() throws Exception {
        super.destroyService();
        // remove me as a listener to the current nx runtime service
        // FIXME: use Framework API instead
        NXRuntime.removeListener(this);
    }

    public void handleEvent(ComponentEvent event) {
        RegistrationInfo ri = event.registrationInfo;
        if (ri == null) {
            return;
        }
        try {
            switch (event.id) {
            case ComponentEvent.COMPONENT_ACTIVATED:
                deployComponent(ri);
                break;
            case ComponentEvent.COMPONENT_DEACTIVATED:
                undeployComponent(ri);
                break;
            }
        } catch (Exception e) {
            log.error("Failed to adapt component", e);
        }
    }

    protected static void deployComponent(RegistrationInfo ri)
            throws MBeanProxyCreationException, MalformedObjectNameException {
        ComponentName name = ri.getName();
        if (log.isDebugEnabled()) {
            log.debug("Registering Mbean for service: " + name);
        }
        RuntimeAdapterMBean rad = (RuntimeAdapterMBean) ServiceLocator
            .getService(RuntimeAdapterMBean.class, RuntimeAdapter.NAME);
        File file = new File(rad.getTempDeployDir(), name + "-service.xml");
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(ComponentMBean
                    .getMBeanXMLContent(name).getBytes());
            out.close();
            if (log.isTraceEnabled()) {
                log.trace("Deploying Mbean file to: " + file.getAbsolutePath());
            }
            // use the ear deployment if any to correctly handle isolation
            DeploymentInfo parent = JBossOSGiAdapter.getEARDeployment();
            DeploymentHelper.deploy(file.toURL(), parent);
        } catch (Exception e) {
            log.error("Failed to register Mbean for service " + ri.getName(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    protected static void undeployComponent(RegistrationInfo ri)
            throws MBeanProxyCreationException, MalformedObjectNameException {
        ComponentName name = ri.getName();
        log.debug("Unregistering Mbean for service: " + name);
        RuntimeAdapterMBean rad = (RuntimeAdapterMBean) ServiceLocator
            .getService(RuntimeAdapterMBean.class, RuntimeAdapter.NAME);
        File file = new File(rad.getTempDeployDir(), name + "-service.xml");
        try {
            DeploymentHelper.undeploy(file.toURL());
        } catch (Exception e) {
            log.error("Failed to un-register Mbean for service " + ri.getName(), e);
        }
    }

    public void handleEvent(RuntimeServiceEvent event) {
        if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
            event.runtime.getComponentManager().addComponentListener(this);
        } else if (event.id == RuntimeServiceEvent.RUNTIME_STOPPED) {
            event.runtime.getComponentManager().removeComponentListener(this);
        }
    }

}
