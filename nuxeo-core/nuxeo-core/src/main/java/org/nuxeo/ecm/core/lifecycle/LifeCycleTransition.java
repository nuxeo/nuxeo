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
 * $Id: LifeCycleTransition.java 19250 2007-05-23 20:06:09Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle;

/**
 * Life cycle transition.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleTransitionImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface LifeCycleTransition {

    /**
     * Returns the transition name.
     *
     * @return the transition name as a string
     */
    String getName();

    /**
     * Sets the transition name.
     *
     * @param name the name of the transition
     */
    void setName(String name);

    /**
     * Returns the description of the transition.
     *
     * @return the description of the transition.
     */
    String getDescription();

    /**
     * Sets the description of the transition.
     *
     * @param description of the transition as a string
     */
    void setDescription(String description);

    /**
     * Returns the destination state.
     *
     * @return the destination state name as a string
     */
    String getDestinationStateName();

    /**
     * Sets the destination state.
     *
     * @param stateName the destination state name
     */
    void setDestinationStateName(String stateName);

}
