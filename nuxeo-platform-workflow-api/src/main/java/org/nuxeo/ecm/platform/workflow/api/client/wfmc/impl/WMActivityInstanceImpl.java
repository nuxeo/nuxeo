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
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMProcessInstance;

/**
 * Activity instance implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class WMActivityInstanceImpl implements WMActivityInstance {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected String rpath;

    protected WMActivityDefinition activityDefinition;

    protected WMProcessInstance instance;

    public WMActivityInstanceImpl() {
    }

    public WMActivityInstanceImpl(String id, String rpath,
            WMActivityDefinition activityDefinition, WMProcessInstance instance) {
        this.id = id;
        this.rpath = rpath;
        this.activityDefinition = activityDefinition;
        this.instance = instance;
    }

    public String getId() {
        return id;
    }

    public WMActivityDefinition getActivityDefinition() {
        return activityDefinition;
    }

    public WMProcessInstance getProcessInstance() {
        return instance;
    }

    public String getRelativePath() {
        return rpath;
    }

    public String getState() {
        // TODO Auto-generated method stub
        return null;
    }

}
