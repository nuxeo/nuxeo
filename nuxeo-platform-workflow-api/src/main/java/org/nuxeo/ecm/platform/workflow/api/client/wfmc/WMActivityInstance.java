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

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;

/**
 * Activity instance.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMActivityInstance extends Serializable {

    /**
     * Returns the activityDefinition instance identifier.
     *
     * @return the activityDefinition instance identifier
     */
    String getId();

    /**
     * Returns the rpath of this activityDefinition.
     *
     * @return the rpath of this activityDefinition
     */
    String getRelativePath();

    /**
     * Returns the activityDefinition definition.
     *
     * @return the corresponding activityDefinition definition
     */
    WMActivityDefinition getActivityDefinition();

    /**
     * Returns the state of this activityDefinition instance.
     *
     * @return the state of this activityDefinition instance
     */
    String getState();

    /**
     * Returns the corresponding process instance.
     *
     * @return the corresponding process instance
     */
    WMProcessInstance getProcessInstance();

}
