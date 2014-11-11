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

package org.nuxeo.ecm.platform.jbpm.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.job.executor.JobExecutor;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.nuxeo.ecm.platform.jbpm.JbpmService;
import org.nuxeo.ecm.platform.jbpm.JbpmTaskService;
import org.nuxeo.ecm.platform.jbpm.ProcessDefinitionDeployer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class JbpmComponent extends DefaultComponent {

    private static final String START_JOB_EXECUTOR = "org.nuxeo.ecm.platform.jbpm.startJobExecutor";

    public enum ConfigurationName {
        jboss, jetty, glassfish, tomcat, tomcatTransactional, tomcatNontransactional
    }

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.jbpm.core.JbpmService");

    public enum ExtensionPoint {
        deployer, processDefinition, activeConfiguration, configurationPath, securityPolicy, typeFilter
    }

    public static final String RUNTIME_CONFIGURATION = "runtime";

    private static final Log log = LogFactory.getLog(JbpmComponent.class);

    private JbpmConfiguration jbpmConfiguration;

    private final Map<String, List<String>> typeFiltersContrib = new HashMap<String, List<String>>();

    private String activeConfigurationName;

    private final Map<String, URL> paths = new HashMap<String, URL>();

    private boolean lazyInitialized;

    private final RuntimeConfigurationSelector selector = new RuntimeConfigurationSelector();

    private final JbpmServiceImpl service = new JbpmServiceImpl();

    private final JbpmTaskServiceImpl taskService = new JbpmTaskServiceImpl();

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
            deployerDesc.put(desc.getName(), desc.getKlass().newInstance());
            break;
        case processDefinition:
            pdDesc.put((ProcessDefinitionDescriptor) contribution, contributor);
            break;
        case activeConfiguration:
            ActiveConfigurationDescriptor descriptor = (ActiveConfigurationDescriptor) contribution;
            activeConfigurationName = descriptor.getName();
            break;
        case configurationPath:
            ConfigurationPathDescriptor configPath = (ConfigurationPathDescriptor) contribution;
            String path = configPath.getPath();
            URL url = contributor.getRuntimeContext().getLocalResource(path);
            if (url == null) {
                throw new RuntimeException("Config not found: " + path);
            }
            if (RUNTIME_CONFIGURATION.equals(configPath.getName())) {
                log.error("'runtime' is a reserved word for configuration. You should use another name for your configuration name");
            }
            paths.put(configPath.getName(), url);
            break;
        case securityPolicy:
            SecurityPolicyDescriptor pmd = (SecurityPolicyDescriptor) contribution;
            service.addSecurityPolicy(pmd.getProcessDefinition(),
                    pmd.getKlass().newInstance());
            break;
        case typeFilter:
            TypeFilterDescriptor tfd = (TypeFilterDescriptor) contribution;
            typeFiltersContrib.put(tfd.getType(), tfd.getPDs());
            break;
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
        ClassLoader nuxeoCL = Framework.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(nuxeoCL);
            ((DbPersistenceServiceFactory) getConfiguration().createJbpmContext().getServiceFactory(
                    Services.SERVICENAME_PERSISTENCE)).getSessionFactory();
        } finally {
            Thread.currentThread().setContextClassLoader(jbossCL);
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

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        JobExecutor jobExecutor = getConfiguration().getJobExecutor();
        if (jobExecutor != null && jobExecutor.isStarted()) {
            jobExecutor.stop();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (JbpmService.class.isAssignableFrom(adapter)) {
            if (service.getConfiguration() == null) {
                synchronized (service) {
                    service.setConfiguration(getConfiguration());
                    initialize();
                }
            }
            return (T) service;
        } else if (JbpmTaskService.class.isAssignableFrom(adapter)) {
            return (T) taskService;
        }
        return null;
    }

    private JbpmConfiguration getConfiguration() {
        if (jbpmConfiguration == null) {
            URL url;
            if (RUNTIME_CONFIGURATION.equals(activeConfigurationName)) {
                String configurationName = selector.getConfigurationName();
                url = paths.get(configurationName);
            } else {
                url = paths.get(activeConfigurationName);
            }
            InputStream is;
            try {
                is = url.openStream();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Unable to open input stream for jbpm configuration "
                                + activeConfigurationName, e);
            }
            jbpmConfiguration = JbpmConfiguration.parseInputStream(is);
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(
                        "Unable to open input stream for jbpm configuration.",
                        e);
            }
        }
        return jbpmConfiguration;
    }

    private void initialize() {
        if (!lazyInitialized) {
            for (Map.Entry<ProcessDefinitionDescriptor, ComponentInstance> entry : pdDesc.entrySet()) {
                ProcessDefinitionDescriptor descriptor = entry.getKey();
                ProcessDefinitionDeployer deployer = deployerDesc.get(descriptor.getDeployer());
                ComponentInstance cmpt = entry.getValue();
                URL url = cmpt.getContext().getResource(descriptor.getPath());
                try {
                    deployer.deploy(url);
                } catch (Exception e) {
                    log.error("error deploying url: " + url, e);
                }
            }
            service.setTypeFilters(typeFiltersContrib);
            boolean startJobExecutor = Boolean.parseBoolean(Framework.getProperty(
                    START_JOB_EXECUTOR, "false"));
            if (startJobExecutor) {
                JobExecutor jobExecutor = getConfiguration().getJobExecutor();
                if (jobExecutor != null) {
                    jobExecutor.start();
                }
            }
        }
    }

}
