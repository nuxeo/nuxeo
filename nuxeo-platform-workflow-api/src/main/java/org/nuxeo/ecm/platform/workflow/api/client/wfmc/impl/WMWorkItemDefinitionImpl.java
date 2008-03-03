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
 * $Id: WMWorkItemDefinitionImpl.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc.impl;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityDefinition;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemDefinition;

/**
 * Work item definition implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMWorkItemDefinitionImpl implements WMWorkItemDefinition {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected String name;

    protected WMActivityDefinition activityDefinition;

    protected String type;

    public WMWorkItemDefinitionImpl() {

    }

    /**
     * @deprecated Use {@link #WMWorkItemDefinitionImpl(String,WMActivityDefinition,String,String)} instead
     */
    @Deprecated
    public WMWorkItemDefinitionImpl(String id,
            WMActivityDefinition activityDefinition, String type) {
        this(id, activityDefinition, type, null);
    }

    public WMWorkItemDefinitionImpl(String id, WMActivityDefinition activityDefinition,
            String type, String name) {
        this.id = id;
        this.activityDefinition = activityDefinition;
        this.type = type;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public WMActivityDefinition getActivityDefinition() {
        return activityDefinition;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
