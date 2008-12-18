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
 * Activity definition.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMActivityDefinition extends Serializable {

    /**
     * Returns the activityDefinition definition name.
     *
     * @return the activityDefinition definition name
     */
    String getName();

    /**
     * Returns the activityDefinition definition description.
     *
     * @return the activityDefinition definition description
     */
    String getDescription();

    /**
     * Returns the available transitions from this activityDefinition.
     *
     * @return an array of available transition instances
     */
    WMTransitionDefinition[] getAvailableTransitions();

    /**
     * Checks if this activityDefinition is holding tasks.
     *
     * @return true if this activityDefinition holds tasks, false otherwise
     */
    boolean isTaskAwareActivity();

    /**
     * Returns the activityDefinition type.
     *
     * @see WMActivityDefinition
     *
     * @return the activityDefinition type
     */
    String getNodeType();

}
