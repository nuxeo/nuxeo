/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.nuxeo.ecm.core.lifecycle.LifeCycleState;
import org.nuxeo.ecm.core.lifecycle.LifeCycleTransition;
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

}
