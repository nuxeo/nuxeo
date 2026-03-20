/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;
import org.nuxeo.runtime.model.Descriptor;
import org.w3c.dom.Element;

/**
 * Descriptor for a life cycle extension.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycle
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
@XObject(value = "lifecycle", order = { "@name" })
public class LifeCycleDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(LifeCycleDescriptor.class);

    @XNode("@name")
    private String name;

    @XNode("@lifecyclemanager")
    public void setLifeCycleManager(String lifeCycleManager) {
        log.warn("Ignoring deprecated lifecyclemanager attribute '{}' for lifecycle '{}'", lifeCycleManager, name);
    }

    @XNode("@initial")
    private String initialStateName;

    @XNode("@defaultInitial")
    private String defaultInitialStateName;

    /** @since 2021.16 */
    @XNode("@enabled")
    private boolean enabled = true;

    @XNode("description")
    private String description;

    @XNode("states")
    private Element states;

    @XNode("transitions")
    private Element transitions;

    @Override
    public String getId() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    /** @since 2021.16 */
    public boolean isEnabled() {
        return enabled;
    }

    public String getInitialStateName() {
        return initialStateName;
    }

    public String getDefaultInitialStateName() {
        return defaultInitialStateName;
    }

    public Collection<LifeCycleState> getStates() {
        LifeCycleStateConfiguration conf = new LifeCycleStateConfiguration(states);
        return conf.getStates();
    }

    public Collection<LifeCycleTransition> getTransitions() {
        return new LifeCycleTransitionConfiguration(transitions).getTransitions();
    }

    /** @since 2021.16 */
    @Override
    public LifeCycleDescriptor merge(Descriptor o) {
        var other = (LifeCycleDescriptor) o;
        var merged = new LifeCycleDescriptor();
        merged.name = getIfNull(other.name, name);
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.initialStateName = getIfNull(other.initialStateName, initialStateName);
        merged.defaultInitialStateName = getIfNull(other.defaultInitialStateName, defaultInitialStateName);
        merged.description = getIfNull(other.description, description);
        merged.states = getIfNull(other.states, states);
        merged.transitions = getIfNull(other.transitions, transitions);
        return merged;
    }
}
