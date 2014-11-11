/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to contribute new versioning rules.
 *
 * @author Laurent Doguin
 * @since 5.4.2
 */
@XObject("versioningRule")
public class VersioningRuleDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeMap(value = "options", key = "@lifeCycleState", type = HashMap.class, componentType = SaveOptionsDescriptor.class)
    public Map<String, SaveOptionsDescriptor> options = new HashMap<String, SaveOptionsDescriptor>();

    @XNode("initialState")
    public InitialStateDescriptor initialState;

    @XNode("@typeName")
    protected String typeName;

    public boolean isEnabled() {
        return enabled;
    }

    public String getTypeName() {
        return typeName;
    }

    public Map<String, SaveOptionsDescriptor> getOptions() {
        return options;
    }

    public InitialStateDescriptor getInitialState() {
        return initialState;
    }

}
