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
 * $Id: WMTransitionDefinition.java 19119 2007-05-22 11:39:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.api.client.wfmc;

import java.io.Serializable;

/**
 * Process transition.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public interface WMTransitionDefinition extends Serializable {

    /**
     * Returns the transition id.
     *
     * @return the transition id
     */
    String getId();

    /**
     * Returns the transition name.
     *
     * @return the transition name
     */
    String getName();

    /**
     * Returns the transition description.
     *
     * @return the transition description
     */
    String getDescription();

    /**
     * Is this the default transition?
     *
     * @return true if this transition is the default one, false otherwise
     */
    boolean isDefaultTransition();

    /**
     * Returns the source activityDefinition for this transition.
     *
     * @return a WMActivityDefinition instance
     */
    WMActivityDefinition getFrom();

    /**
     * Returns the destination activityDefinition for this transition.
     *
     * @return a WMActivityDefinition instance
     */
    WMActivityDefinition getTo();

}
