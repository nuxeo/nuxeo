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
 *     alexandre
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.nuxeo.ecm.platform.jbpm.core.deployer.ProcessDefinitionDeployer;
import org.nuxeo.ecm.platform.jbpm.core.service.BPMManagementServiceImpl;
import org.nuxeo.ecm.platform.jbpm.core.service.DeployerDescriptor;
import org.nuxeo.ecm.platform.jbpm.core.service.ProcessDefinitionDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class JbpmComponent extends DefaultComponent {
    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.jbpm.core.JbpmService");

    public static enum ExtensionPoint {
        deployer, processDefinition
    };

    private final BPMManagementServiceImpl bManagementServiceImpl = new BPMManagementServiceImpl();

    private static final Log log = LogFactory.getLog(JbpmComponent.class);

    private final HashMap<String, ProcessDefinitionDeployer> deployerDesc = new HashMap<String, ProcessDefinitionDeployer>();

    private final HashMap<ProcessDefinitionDescriptor, ComponentInstance> pdDesc = new HashMap<ProcessDefinitionDescriptor, ComponentInstance>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        ExtensionPoint ep = Enum.valueOf(ExtensionPoint.class, extensionPoint);
        switch (ep) {
        case deployer:
            DeployerDescriptor desc = (DeployerDescriptor) contribution;
            deployerDesc.put(desc.getName(), (ProcessDefinitionDeployer) desc.getKlass().newInstance());
            break;
        case processDefinition:
            pdDesc.put((ProcessDefinitionDescriptor) contribution, contributor);
            break;
        }
    }

    @Override
    public void activate(ComponentContext context) throws Exception {
        for (Map.Entry<ProcessDefinitionDescriptor, ComponentInstance> entry : pdDesc.entrySet()) {
            ProcessDefinitionDescriptor pdDescriptor = entry.getKey();
            ProcessDefinitionDeployer deployer = deployerDesc.get(pdDescriptor.getDeployer());
            if (deployer == null) {
                log.warn("No deployer named '" + entry.getKey().getDeployer()
                        + "' have been registered.");
                continue;
            }
            URL url = entry.getValue().getRuntimeContext().getResource(
                    pdDescriptor.getPath());
            if (deployer.isDeployable(url)) {
                log.debug("Deploying process definition: " + url.getPath());
                deployer.deploy(url);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (BPMManagementService.class.isAssignableFrom(adapter)) {
            if (bManagementServiceImpl.getConfiguration() == null) {
                synchronized (bManagementServiceImpl) {
                    bManagementServiceImpl.setConfiguration(getConfiguration());
                }
            }
            return (T) bManagementServiceImpl;
        }
        return null;
    }

    private JbpmConfiguration getConfiguration() {
        JBPMConfiguration configuration = null;
        try {
            configuration = Framework.getService(JBPMConfiguration.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get jbpm configuration service.", e);
        }
        URL url = configuration.getActiveConfigurationFile();
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            throw new RuntimeException("Unable to open input stream for jbpm configuration.", e);
        }
        JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseInputStream(is);
        try {
            is.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to open input stream for jbpm configuration.", e);
        }
        return jbpmConfiguration;
    }
}
