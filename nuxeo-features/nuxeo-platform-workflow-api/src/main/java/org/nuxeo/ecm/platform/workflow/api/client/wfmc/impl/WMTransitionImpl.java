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

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMTransitionDefinition;

/**
 * Process transition implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMTransitionImpl implements WMTransitionDefinition {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected String name;

    protected String description;

    protected boolean defaultTransition;

    protected WMActivityDefinition activityFrom;

    protected WMActivityDefinition activityTo;

    public WMTransitionImpl() {
    }

    public WMTransitionImpl(String id, String name, String description,
            boolean defaultTransition, WMActivityDefinition nodeFrom,
            WMActivityDefinition nodeTo) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultTransition = defaultTransition;
        this.activityFrom = nodeFrom;
        this.activityTo = nodeTo;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDefaultTransition() {
        return defaultTransition;
    }

    public WMActivityDefinition getFrom() {
        return activityFrom;
    }

    public WMActivityDefinition getTo() {
        return activityTo;
    }

}
