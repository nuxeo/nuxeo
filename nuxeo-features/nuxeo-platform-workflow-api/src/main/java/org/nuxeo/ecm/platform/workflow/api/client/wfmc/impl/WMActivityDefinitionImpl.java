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
 * $Id: WMActivityDefinitionImpl.java 21821 2007-07-03 08:59:32Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMTransitionDefinition;

/**
 * Activity definition implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMActivityDefinitionImpl implements WMActivityDefinition {

    private static final long serialVersionUID = 1L;

    protected String name;

    protected String description;

    protected WMTransitionDefinition[] transitions;

    protected boolean taskAware;

    protected String nodeType;

    public WMActivityDefinitionImpl() {
    }

    public WMActivityDefinitionImpl(String name, String description,
            WMTransitionDefinition[] transitions, boolean taskAware,
            String nodeType) {
        this.name = name;
        this.description = description;
        this.transitions = transitions;
        this.taskAware = taskAware;
        this.nodeType = nodeType;
    }

    public WMTransitionDefinition[] getAvailableTransitions() {
        return transitions;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getNodeType() {
        return nodeType;
    }

    public boolean isTaskAwareActivity() {
        return taskAware;
    }

}
