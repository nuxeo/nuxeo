/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
    protected Boolean enabled;

    @XNodeMap(value = "options", key = "@lifeCycleState", type = HashMap.class, componentType = SaveOptionsDescriptor.class)
    public Map<String, SaveOptionsDescriptor> options = new HashMap<>();

    @XNode("initialState")
    public InitialStateDescriptor initialState;

    @XNode("@typeName")
    protected String typeName;

    /** True if the boolean is null or TRUE, false otherwise. */
    private static boolean defaultTrue(Boolean bool) {
        return !Boolean.FALSE.equals(bool);
    }

    public boolean isEnabled() {
        return defaultTrue(enabled);
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

    /** Empty constructor. */
    public VersioningRuleDescriptor() {
    }

    /** Copy constructor. */
    public VersioningRuleDescriptor(VersioningRuleDescriptor other) {
        this.enabled = other.enabled;
        this.typeName = other.typeName;
        this.options = other.options;
        this.initialState = other.initialState;
    }

    public void merge(VersioningRuleDescriptor other) {
        if (other.enabled != null) {
            enabled = other.enabled;
        }
        if (other.typeName != null) {
            typeName = other.typeName;
        }
        options.putAll(other.options); // always merge options TODO override flag
        if (other.initialState != null) {
            initialState = other.initialState;
        }
    }

}
