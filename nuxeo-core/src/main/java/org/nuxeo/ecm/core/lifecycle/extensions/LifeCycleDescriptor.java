/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Anguenot
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.lifecycle.LifeCycle;
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;
import org.nuxeo.ecm.core.lifecycle.impl.LifeCycleImpl;
import org.w3c.dom.Element;

/**
 * Descriptor for a life cycle extension.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 * @see org.nuxeo.ecm.core.lifecycle.LifeCycle
 *
 * @author Julien Anguenot
 * @author Florent Guillaume
 */
@XObject(value = "lifecycle", order = { "@name" })
public class LifeCycleDescriptor {

    private static final Log log = LogFactory.getLog(LifeCycleDescriptor.class);

    /** Name of this life cycle. */
    @XNode("@name")
    private String name;

    @XNode("@lifecyclemanager")
    public void setLifeCycleManager(String lifeCycleManager) {
        log.warn("Ignoring deprecated lifecyclemanager attribute '" +
                lifeCycleManager + "' for lifecycle '" + name + "'");
    }

    /** The initial state name. */
    @XNode("@initial")
    private String initialStateName;

    @XNode("description")
    private String description;

    @XNode("states")
    private Element states;

    @XNode("transitions")
    private Element transitions;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public Collection<LifeCycleState> getStates() {
        LifeCycleStateConfiguration conf = new LifeCycleStateConfiguration(
                states);
        return conf.getStates();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStates(Element states) {
        this.states = states;
    }

    public void setTransitions(Element transitions) {
        this.transitions = transitions;
    }

    public Collection<LifeCycleTransition> getTransitions() {
        return new LifeCycleTransitionConfiguration(transitions)
                .getTransitions();
    }

    public String getInitialStateName() {
        return initialStateName;
    }

    /**
     * Returns a life cycle instance out of the life cycle configuration.
     *
     * @return a life cycle instance out of the life cycle configuration.
     */
    public LifeCycle getLifeCycle() {
        return new LifeCycleImpl(name, initialStateName, getStates(),
                getTransitions());
    }

    public void setInitialStateName(String initialStateName) {
        this.initialStateName = initialStateName;
    }

}
