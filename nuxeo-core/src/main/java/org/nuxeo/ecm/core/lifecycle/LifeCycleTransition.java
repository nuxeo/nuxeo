/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
     * Returns the description of the transition.
     *
     * @return the description of the transition.
     */
    String getDescription();

    /**
     * Returns the destination state.
     *
     * @return the destination state name as a string
     */
    String getDestinationStateName();

}
