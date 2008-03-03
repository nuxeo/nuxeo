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
 * $Id: WorkflowService.java 28460 2008-01-03 15:34:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.service;

import java.io.InputStream;
import java.net.URL;

import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessDefinitionState;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;

/**
 * Workflow service.
 * <p>
 * Deals with the deployments of definitions within given engine backends.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WorkflowService {

    /**
     * Returns the default engine name.
     *
     * @return the default engine name
     */
    String getDefaultEngineName();

    /**
     * Sets the default engine name.
     *
     * @param name the default engine name
     */
    void setDefaultEngineName(String name);

    /**
     * Registers a workflow engine.
     *
     * @param engine the workflow engine to register
     */
    void registerWorkflowEngine(WorkflowEngine engine);

    /**
     * Unregisters a workflow engine.
     *
     * @param name the workflow engine name to unregister
     */
    void unregisterWorkflowEngine(String name);

    /**
     * Returns a workflow engine by name.
     *
     * @param name of the workflow engine
     * @return the workflow engine
     */
    WorkflowEngine getWorkflowEngineByName(String name);

    /**
     * Deploys a definition within a given engine.
     *
     * @param engineName
     * @param definitionURL
     * @param mimetype
     * @return the corresponding definition id.
     * @throws WMWorkflowException
     *             TODO
     * @deprecated Use {@link #deployDefinition(String,InputStream,String)} instead
     */
    @Deprecated
    WMProcessDefinitionState deployDefinition(String engineName,
            URL definitionURL, String mimetype) throws WMWorkflowException;

    /**
     * Deploys a definition within a given engine.
     *
     * @param engineName
     * @param stream
     * @param mimetype
     * @return the corresponding definition id.
     * @throws WMWorkflowException
     *             TODO
     */
    WMProcessDefinitionState deployDefinition(String engineName,
            InputStream stream, String mimetype) throws WMWorkflowException;

    /**
     * Undeploys a given workflow definition within an engine given its id.
     *
     * @param engineName
     * @param workflowDefinitionId
     * @throws WMWorkflowException
     *             TODO
     */
    void undeployDefinition(String engineName,
            String workflowDefinitionId) throws WMWorkflowException;

    /**
     * Is a given workflow definition deployed within a given engine?
     *
     * @param engineName
     * @param workflowDefinitionId
     * @return true if the definition is deployed, false otherwise
     */
    boolean isDefinitionDeployed(String engineName,
            String workflowDefinitionId);

}
