/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.ecm.core.lifecycle.impl;

import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;

/**
 * Life cycle transition implementation.
 *
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycleTransition
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class LifeCycleTransitionImpl implements LifeCycleTransition {

    /** Name of the transition. */
    private final String name;

    /** Description of the transition. */
    private final String description;

    /** Destination state name for this transition. */
    private final String destinationStateName;

    public LifeCycleTransitionImpl(String name, String description, String destinationState) {
        this.name = name;
        this.description = description;
        destinationStateName = destinationState;
    }

    @Override
    public String getDestinationStateName() {
        return destinationStateName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

}
