/*
 * (C) Copyright 2006-2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Anguenot
 *     Florent Guillaume
 *     Anahide Tchertchian
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;

/**
 * Descriptor for a life cycle extension.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycle
 */
@XObject(value = "lifecycle", order = { "@name" })
@XRegistry(compatWarnOnMerge = true)
public class LifeCycleDescriptor {

    @XNode(value = "@name", defaultAssignment = "")
    @XRegistryId
    protected String name;

    @XNode("@initial")
    protected String initialStateName;

    @XNode("@defaultInitial")
    protected String defaultInitialStateName;

    @XNode("description")
    protected String description;

    @XNodeList(value = "states/state", type = ArrayList.class, componentType = LifeCycleStateDescriptor.class)
    protected List<LifeCycleStateDescriptor> states;

    @XNodeList(value = "transitions/transition", type = ArrayList.class, componentType = LifeCycleTransitionDescriptor.class)
    protected List<LifeCycleTransitionDescriptor> transitions;

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getInitialStateName() {
        return initialStateName;
    }

    public String getDefaultInitialStateName() {
        return defaultInitialStateName;
    }

    public Collection<LifeCycleState> getStates() {
        return Collections.unmodifiableList(states);
    }

    public Collection<LifeCycleTransition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }

}
