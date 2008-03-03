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
 * $Id: WorkflowServiceImpl.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinitionState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;
import org.nuxeo.ecm.platform.workflow.service.extensions.WorkflowDefinitionDeploymentDescriptor;
import org.nuxeo.ecm.platform.workflow.service.extensions.WorkflowEngineDescriptor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * Workflow service implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkflowServiceImpl extends DefaultComponent implements
        WorkflowService {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.workflow.service.WorkflowService");

    private static final Log log = LogFactory.getLog(WorkflowServiceImpl.class);

    private final WorkflowEngineRegistry engines = new WorkflowEngineRegistry();

    private String defaultEngine;

    private RuntimeContext context;

    @Override
    public void activate(ComponentContext context) {
        this.context = context.getRuntimeContext();
    }

    public String getDefaultEngineName() {
        return defaultEngine;
    }

    public WorkflowEngine getWorkflowEngineByName(String name) {
        return engines.getWorkflowEngineByName(name);
    }

    @Override
    public void registerExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {

            // Workflow engine extension point.
            if (extension.getExtensionPoint().equals("engine")) {
                for (Object contribution : contributions) {

                    WorkflowEngineDescriptor desc = (WorkflowEngineDescriptor) contribution;
                    Class<WorkflowEngine> klass = desc.getClassName();

                    if (klass != null) {

                        WorkflowEngine engine = klass.newInstance();
                        engine.setName(desc.getName());

                        // Set this engine as the default engine
                        if (desc.isDefaultEngine()) {
                            defaultEngine = engine.getName();
                        }
                        log.info("Trying to register workflow engine with name="
                                + engine.getName());
                        registerWorkflowEngine(engine);
                    } else {
                        log.error("###################################");
                        log.error("Cannot load klass=" + desc.getName());
                        log.error("###################################");
                    }
                }
            }

            // Workflow definition extension point
            if (extension.getExtensionPoint().equals("definition")) {
                for (Object contribution : contributions) {
                    WorkflowDefinitionDeploymentDescriptor desc = (WorkflowDefinitionDeploymentDescriptor) contribution;
                    desc.context = extension.getComponent().getContext();

                    log.info("Definition path = " + desc.getDefinitionPath());

                    RuntimeContext definitionContext = desc.context == null ? context
                            : desc.context;
                    URL definitionUrl = definitionContext.getResource(desc.getDefinitionPath());

                    log.info("Request definition deployment in engine="
                            + desc.getEngineName());
                    if (definitionUrl != null) {

                        log.info("Definition URL @ " + definitionUrl.getPath());
                        deployDefinition(desc.getEngineName(),
                                definitionUrl.openStream(), desc.getMimetype());
                    } else {
                        log.error("Definition not found : cancel registration....");
                    }
                }
            }
        }
    }

    public void registerWorkflowEngine(WorkflowEngine engine) {
        if (engine.getName() != null) {
            log.info("Register a new workflow engine with name="
                    + engine.getName());
            engines.register(engine);
        } else {
            log.error("Impossible to register an engine with no name...");
        }
    }

    public void setDefaultEngineName(String name) {
        defaultEngine = name;
    }

    @Override
    public void unregisterExtension(Extension extension) throws Exception {
        Object[] contributions = extension.getContributions();
        if (contributions != null) {
            if (extension.getExtensionPoint().equals("engine")) {
                for (Object contribution : contributions) {
                    WorkflowEngineDescriptor desc = (WorkflowEngineDescriptor) contribution;
                    log.info("Trying to unregister workflow engine with name="
                            + desc.getName());
                    unregisterWorkflowEngine(desc.getName());
                }
            } else if (extension.getExtensionPoint().equals("definition")) {
                log.info("Trying to unregister workflow definition");
                /*
                 * for (Object contribution : contributions) {
                 * WorkflowDefinitionDeploymentDescriptor desc =
                 * (WorkflowDefinitionDeploymentDescriptor) contribution; //
                 * :XXX: }
                 */
            } else {
                log.error("Unknown contributions... can't unregister !");
            }
        }
    }

    public void unregisterWorkflowEngine(String name) {
        log.info("Unregister a workflow engine with name=" + name);
        engines.unregister(name);
    }

    /**
     * @deprecated Use {@link #deployDefinition(String,InputStream,String)}
     *             instead
     */
    @Deprecated
    public WMProcessDefinitionState deployDefinition(String engineName,
            URL definitionURL, String mimetype) throws WMWorkflowException {
        try {
            return deployDefinition(engineName, definitionURL.openStream(),
                    mimetype);
        } catch (IOException e) {
            throw new WMWorkflowException(e);
        }
    }

    public WMProcessDefinitionState deployDefinition(String engineName,
            InputStream stream, String mimetype) throws WMWorkflowException {
        WorkflowEngine engine;
        if (engineName == null) {
            engine = getWorkflowEngineByName(defaultEngine);
        } else {
            engine = getWorkflowEngineByName(engineName);
        }
        if (engine == null) {
            log.error("No suitable workflow engine for deployment found !");
            return null;
        }
        WMProcessDefinitionState deployment = engine.deployDefinition(stream,
                mimetype);
        return deployment;
    }

    public boolean isDefinitionDeployed(String engineName,
            String workflowDefinitionId) {
        WorkflowEngine engine;
        if (engineName == null) {
            engine = getWorkflowEngineByName(defaultEngine);
        } else {
            engine = getWorkflowEngineByName(engineName);
        }
        boolean found = false;
        if (engine == null) {
            log.error("No suitable workflow engine for deployment found !");
        } else {
            found = engine.isDefinitionDeployed(workflowDefinitionId);
        }
        return found;
    }

    public void undeployDefinition(String engineName,
            String workflowDefinitionId) throws WMWorkflowException {
        WorkflowEngine engine;
        if (engineName == null) {
            engine = getWorkflowEngineByName(defaultEngine);
        } else {
            engine = getWorkflowEngineByName(engineName);
        }
        if (engine == null) {
            log.error("No suitable workflow engine for deployment found !");
        } else {
            engine.undeployDefinition(workflowDefinitionId);
        }
    }

}
