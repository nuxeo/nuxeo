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
import java.util.HashSet;

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

    @XNode("@name")
    private String name;

    @XNode("@lifecyclemanager")
    public void setLifeCycleManager(String lifeCycleManager) {
        log.warn("Ignoring deprecated lifecyclemanager attribute '"
                + lifeCycleManager + "' for lifecycle '" + name + "'");
    }

    @XNode("@initial")
    private String initialStateName;

    @XNode("@defaultInitial")
    private String defaultInitialStateName;

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

    public String getInitialStateName() {
        return initialStateName;
    }

    public String getDefaultInitialStateName() {
        return defaultInitialStateName;
    }

    public Collection<LifeCycleState> getStates() {
        LifeCycleStateConfiguration conf = new LifeCycleStateConfiguration(
                states);
        return conf.getStates();
    }

    public Collection<LifeCycleTransition> getTransitions() {
        return new LifeCycleTransitionConfiguration(transitions).getTransitions();
    }

    /**
     * Returns a life cycle instance out of the life cycle configuration.
     */
    public LifeCycle getLifeCycle() {
        String defaultInitialStateName = this.defaultInitialStateName;
        if (initialStateName != null) {
            defaultInitialStateName = initialStateName;
            log.warn(String.format("Lifecycle registration of default initial"
                    + " state has changed, change initial=\"%s\" to "
                    + "defaultInitial=\"%s\" in lifecyle '%s' definition",
                    defaultInitialStateName, defaultInitialStateName, name));
        }
        boolean defaultInitialStateFound = false;
        Collection<String> initialStateNames = new HashSet<String>();
        Collection<LifeCycleState> states = getStates();
        for (LifeCycleState state : states) {
            String stateName = state.getName();
            if (defaultInitialStateName.equals(stateName)) {
                defaultInitialStateFound = true;
                initialStateNames.add(stateName);
            }
            if (state.isInitial()) {
                initialStateNames.add(stateName);
            }
        }
        if (!defaultInitialStateFound) {
            log.error(String.format(
                    "Default initial state %s not found on lifecycle %s",
                    defaultInitialStateName, name));
        }
        return new LifeCycleImpl(name, defaultInitialStateName,
                initialStateNames, states, getTransitions());
    }

}
