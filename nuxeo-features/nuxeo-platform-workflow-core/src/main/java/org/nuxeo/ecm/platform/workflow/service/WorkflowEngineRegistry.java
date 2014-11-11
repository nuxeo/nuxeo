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
 * $Id: WorkflowEngineRegistry.java 20788 2007-06-19 08:16:55Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.workflow.api.WorkflowEngine;

/**
 * Workflow engine registry.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WorkflowEngineRegistry {

    // :XXX: factor out with action handler registry

    static final Log log = LogFactory.getLog(WorkflowEngineRegistry.class);

    private final Map<String, WorkflowEngine> registry;

    public WorkflowEngineRegistry() {
        registry = new HashMap<String, WorkflowEngine>();
    }

    public void register(WorkflowEngine engine) {
        if (!isRegistered(engine.getName()) && !isRegistered(engine)) {
            registry.put(engine.getName(), engine);
        }
    }

    public void unregister(String name) {
        if (isRegistered(name)) {
            registry.remove(name);
        }
    }

    public boolean isRegistered(WorkflowEngine engine) {
        return registry.containsValue(engine);
    }

    public boolean isRegistered(String name) {
        return registry.containsKey(name);
    }

    public int size() {
        return registry.size();
    }

    public WorkflowEngine getWorkflowEngineByName(String name) {
        return registry.get(name);
    }

    public void clear() {
        registry.clear();
    }

}
