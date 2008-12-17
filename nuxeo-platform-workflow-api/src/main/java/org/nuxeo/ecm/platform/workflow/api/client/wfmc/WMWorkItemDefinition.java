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
 * $Id: WMWorkItemDefinition.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;

/**
 * Work item definition.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMWorkItemDefinition extends Serializable {

    /**
     * Returns the task definition identifier.
     *
     * @return the task definition identifier
     */
    String getId();

    /**
     * Returns the task definition name.
     *
     * @return the task definition name
     */
    String getName();

    /**
     * Returns the activityDefinition definition on which the work item is defined.
     *
     * @return a activityDefinition definition
     */
    WMActivityDefinition getActivityDefinition();

    /**
     * Returns the work item definition type.
     *
     * @see org.nuxeo.ecm.platform.workflow.api.common.WorkflowConstants
     *
     * @return the work item definition type
     */
    String getType();


}
